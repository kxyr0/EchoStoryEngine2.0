package io.github.kxyr0.echotrigger.core.runtime.reactive;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackDefinition;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.policy.TriggerFirePolicy;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.ReactiveTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.TriggerInstanceIds;
import io.github.kxyr0.echotrigger.core.runtime.TriggerRuntimeInstance;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TriggerReactiveRuntime {
    private final Map<String, Boolean> memberships = new LinkedHashMap<>();
    private final Map<String, Integer> fireCounts = new LinkedHashMap<>();
    private final Map<String, EnumSet<TriggerCallbackKind>> consumedCallbacks = new LinkedHashMap<>();
    private final Map<String, Long> cooldownUntil = new LinkedHashMap<>();
    private final Map<String, Long> nextStayTick = new LinkedHashMap<>();

    public List<TriggerCallbackInvocation> apply(
            Movement movement,
            TriggerDefinitionCatalog catalog,
            Map<TriggerInstanceId, TriggerRuntimeInstance> instances) {
        List<TriggerCallbackInvocation> invocations = new ArrayList<>();
        for (CompiledTriggerDefinition definition : catalog.definitions()) {
            definition.reactive().flatMap(reactive -> attempt(definition, reactive, movement, instances))
                    .ifPresent(invocations::add);
        }
        return List.copyOf(invocations);
    }

    private Optional<TriggerCallbackInvocation> attempt(
            CompiledTriggerDefinition definition, ReactiveTriggerDefinition reactive,
            Movement movement,
            Map<TriggerInstanceId, TriggerRuntimeInstance> instances) {
        String key = key(definition, movement.owner());
        boolean currentInside = reactive.primaryArea().contains(movement.currentDimension(), movement.current());
        Optional<TriggerCallbackKind> kind = transitionKind(key, reactive, movement, currentInside);
        remember(key, reactive, movement, currentInside, kind);
        return kind.flatMap(value -> invocation(definition, reactive, movement, instances, value));
    }

    private Optional<TriggerCallbackInvocation> invocation(
            CompiledTriggerDefinition definition,
            ReactiveTriggerDefinition reactive,
            Movement movement,
            Map<TriggerInstanceId, TriggerRuntimeInstance> instances,
            TriggerCallbackKind kind) {
        TriggerCallbackDefinition callback = reactive.callbacks().get(kind);
        if (callback == null || !accepted(key(definition, movement.owner()), kind, reactive.firePolicy(), movement)) {
            return Optional.empty();
        }
        TriggerInstanceId instanceId = TriggerInstanceIds.instance(definition.id(), movement.owner(),
                definition.revision());
        instances.computeIfAbsent(instanceId, ignored -> newInstance(definition, instanceId, movement.owner()));
        recordFire(key(definition, movement.owner()), kind, reactive.firePolicy(), movement.serverTick());
        return Optional.of(new TriggerCallbackInvocation(definition.id(), instanceId, movement.owner(), kind,
                callback.signature(), callback.actions(), payload(reactive, kind, movement), movement.serverTick()));
    }

    private Optional<TriggerCallbackKind> transitionKind(
            String key, ReactiveTriggerDefinition reactive,
            Movement movement,
            boolean currentInside) {
        if (!memberships.containsKey(key) && movement.previous().equals(movement.current())) {
            return Optional.empty();
        }
        boolean previousInside = memberships.getOrDefault(key,
                reactive.primaryArea().contains(movement.previousDimension(), movement.previous()));
        if (!previousInside && currentInside) {
            return Optional.of(TriggerCallbackKind.ENTER);
        }
        if (previousInside && !currentInside) {
            return Optional.of(TriggerCallbackKind.EXIT);
        }
        return previousInside && currentInside ? stayKind(key, reactive, movement) : Optional.empty();
    }

    private Optional<TriggerCallbackKind> stayKind(
            String key,
            ReactiveTriggerDefinition reactive,
            Movement movement) {
        if (!reactive.callbacks().containsKey(TriggerCallbackKind.STAY)) {
            return Optional.empty();
        }
        long due = nextStayTick.getOrDefault(key, movement.serverTick() + reactive.stayIntervalTicks());
        if (movement.serverTick() < due) {
            return Optional.empty();
        }
        nextStayTick.put(key, movement.serverTick() + reactive.stayIntervalTicks());
        return Optional.of(TriggerCallbackKind.STAY);
    }

    private void remember(
            String key,
            ReactiveTriggerDefinition reactive,
            Movement movement,
            boolean currentInside,
            Optional<TriggerCallbackKind> kind) {
        memberships.put(key, currentInside);
        if (!currentInside) {
            nextStayTick.remove(key);
        } else if (kind.filter(value -> value == TriggerCallbackKind.STAY).isEmpty()) {
            nextStayTick.putIfAbsent(key, movement.serverTick() + reactive.stayIntervalTicks());
        }
        kind.filter(value -> value == TriggerCallbackKind.ENTER)
                .ifPresent(value -> nextStayTick.put(key, movement.serverTick() + reactive.stayIntervalTicks()));
    }

    private boolean accepted(
            String key,
            TriggerCallbackKind kind,
            TriggerFirePolicy policy,
            Movement movement) {
        int count = fireCounts.getOrDefault(key, 0);
        boolean fireCountReached = policy.maxFireCount().isPresent()
                && count >= policy.maxFireCount().orElseThrow();
        boolean onceConsumed = policy.oneShotCallbacks().contains(kind)
                && consumedCallbacks.getOrDefault(key, EnumSet.noneOf(TriggerCallbackKind.class)).contains(kind);
        if (!cooldownReady(key, movement.serverTick())) {
            return false;
        }
        if (policy.fireOnlyOnce() && count > 0) {
            return false;
        }
        return !fireCountReached && !onceConsumed;
    }

    private void recordFire(String key, TriggerCallbackKind kind, TriggerFirePolicy policy, long serverTick) {
        fireCounts.put(key, fireCounts.getOrDefault(key, 0) + 1);
        if (policy.onceFor(kind)) {
            consumedCallbacks.computeIfAbsent(key, ignored -> EnumSet.noneOf(TriggerCallbackKind.class)).add(kind);
        }
        if (policy.cooldownTicks() > 0) {
            cooldownUntil.put(key, serverTick + policy.cooldownTicks());
        }
    }

    private boolean cooldownReady(String key, long serverTick) {
        return serverTick >= cooldownUntil.getOrDefault(key, 0L);
    }

    private TriggerRuntimeInstance newInstance(
            CompiledTriggerDefinition definition,
            TriggerInstanceId instanceId,
            TriggerOwnerId owner) {
        return new TriggerRuntimeInstance(definition, instanceId, TriggerInstanceIds.execution(instanceId, 0L),
                owner, 0, TriggerStatus.RUNNING, TriggerOutcome.NONE, 0L);
    }

    private String key(CompiledTriggerDefinition definition, TriggerOwnerId owner) {
        return TriggerInstanceIds.instance(definition.id(), owner, definition.revision()).value();
    }

    private Map<String, String> payload(
            ReactiveTriggerDefinition reactive,
            TriggerCallbackKind kind,
            Movement movement) {
        return Map.of("dimension", movement.currentDimension(),
                "previousDimension", movement.previousDimension(),
                "area", reactive.primaryArea().fieldName(),
                "callback", kind.name(),
                "position", movement.current().stableText(),
                "previousPosition", movement.previous().stableText(),
                "shape", reactive.primaryArea().shape().kind().name());
    }

    public record Movement(
            TriggerOwnerId owner,
            String previousDimension,
            String currentDimension,
             Vector3 previous,
             Vector3 current,
             long serverTick) {
        public static Movement sameDimension(
                TriggerOwnerId owner,
                 String dimension,
                 Vector3 previous,
                 Vector3 current,
                 long serverTick) {
            return new Movement(owner, dimension, dimension, previous, current, serverTick);
        }
    }
}
