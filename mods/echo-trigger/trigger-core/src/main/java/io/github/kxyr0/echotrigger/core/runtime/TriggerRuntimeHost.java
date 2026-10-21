package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.reactive.TriggerReactiveRuntime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TriggerRuntimeHost {
    private final TriggerCommandLedger ledger = new TriggerCommandLedger();
    private final TriggerSequenceCursor cursor = new TriggerSequenceCursor();
    private final TriggerReactiveRuntime reactiveRuntime = new TriggerReactiveRuntime();
    private final Map<TriggerInstanceId, TriggerRuntimeInstance> instances = new LinkedHashMap<>();
    private TriggerDefinitionCatalog catalog;

    public TriggerRuntimeHost(TriggerDefinitionCatalog catalog) {
        this.catalog = catalog.runtimeView();
    }

    public void publishCatalog(TriggerDefinitionCatalog catalog) {
        this.catalog = catalog.runtimeView();
    }

    public TriggerResult start(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return ledger.apply(commandId, "start|" + id + "|" + owner.value(), () -> startNow(id, owner));
    }

    public TriggerResult stop(TriggerInstanceId instanceId, TriggerCommandId commandId) {
        return ledger.apply(commandId, "stop|" + instanceId.value(), () -> stopNow(instanceId));
    }

    public TriggerResult reset(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return ledger.apply(commandId, "reset|" + id + "|" + owner.value(), () -> resetNow(id, owner));
    }

    public TriggerResult fire(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return ledger.apply(commandId, "fire|" + id + "|" + owner.value(), () -> fireNow(id, owner));
    }

    public List<TriggerSnapshot> applyEvent(TriggerEvent event) {
        return instances.values().stream()
                .map(instance -> cursor.apply(instance, event))
                .peek(instance -> instances.put(instance.instanceId(), instance))
                .map(TriggerRuntimeInstance::snapshot)
                .toList();
    }

    public List<TriggerCallbackInvocation> applyMovement(
            TriggerOwnerId owner,
            String dimension,
            Vector3 previous,
            Vector3 current,
            long serverTick
    ) {
        return applyMovement(TriggerReactiveRuntime.Movement.sameDimension(owner, dimension, previous, current,
                serverTick));
    }

    public List<TriggerCallbackInvocation> applyMovement(TriggerReactiveRuntime.Movement movement) {
        return reactiveRuntime.apply(movement, catalog, instances);
    }

    public Optional<TriggerSnapshot> query(TriggerInstanceId instanceId) {
        return Optional.ofNullable(instances.get(instanceId)).map(TriggerRuntimeInstance::snapshot);
    }

    public int clearRuntime() {
        int removed = instances.size();
        instances.clear();
        ledger.clear();
        return removed;
    }

    public int clearRuntime(TriggerOwnerId owner) {
        int before = instances.size();
        instances.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
        ledger.clear();
        return before - instances.size();
    }

    public int clearRuntime(TriggerId id) {
        int before = instances.size();
        instances.entrySet().removeIf(entry -> entry.getValue().definition().id().equals(id));
        ledger.clear();
        return before - instances.size();
    }

    public int clearRuntime(TriggerId id, TriggerOwnerId owner) {
        int before = instances.size();
        instances.entrySet().removeIf(entry -> entry.getValue().definition().id().equals(id)
                && entry.getValue().owner().equals(owner));
        ledger.clear();
        return before - instances.size();
    }

    public long duplicateCount(TriggerId id, TriggerOwnerId owner) {
        return instances.values().stream()
                .filter(instance -> instance.definition().id().equals(id) && instance.owner().equals(owner))
                .count() - 1;
    }

    private TriggerResult startNow(TriggerId id, TriggerOwnerId owner) {
        Optional<CompiledTriggerDefinition> definition = catalog.find(id);
        if (definition.isEmpty()) {
            return TriggerResult.rejected(TriggerResultCode.NOT_FOUND, "Trigger definition not found: " + id);
        }
        TriggerInstanceId instanceId = TriggerInstanceIds.instance(id, owner, definition.orElseThrow().revision());
        TriggerRuntimeInstance existing = instances.get(instanceId);
        if (existing != null) {
            if (existing.status() != TriggerStatus.STOPPED) {
                return TriggerResult.of(TriggerResultCode.EXISTING, existing.snapshot());
            }
            instances.remove(instanceId);
        }
        TriggerRuntimeInstance instance = newInstance(definition.orElseThrow(), instanceId, owner, 0);
        instances.put(instanceId, instance);
        return TriggerResult.of(TriggerResultCode.STARTED, instance.snapshot());
    }

    private TriggerResult stopNow(TriggerInstanceId instanceId) {
        TriggerRuntimeInstance instance = instances.get(instanceId);
        if (instance == null) {
            return TriggerResult.rejected(TriggerResultCode.NOT_FOUND, "Trigger instance not found.");
        }
        TriggerRuntimeInstance stopped = instance.stopped();
        instances.put(instanceId, stopped);
        return TriggerResult.of(TriggerResultCode.STOPPED, stopped.snapshot());
    }

    private TriggerResult resetNow(TriggerId id, TriggerOwnerId owner) {
        catalog.find(id).ifPresent(definition -> instances.remove(TriggerInstanceIds.instance(
                id, owner, definition.revision())));
        return startNow(id, owner);
    }

    private TriggerResult fireNow(TriggerId id, TriggerOwnerId owner) {
        Optional<CompiledTriggerDefinition> definition = catalog.find(id);
        if (definition.isEmpty() || definition.orElseThrow().manualFire() != TriggerManualFirePolicy.ALLOW) {
            return TriggerResult.rejected(TriggerResultCode.POLICY_DENIED, "Manual fire is not allowed.");
        }
        TriggerResult started = startNow(id, owner);
        Optional<TriggerInstanceId> instanceId = started.instanceId();
        if (instanceId.isEmpty()) {
            return started;
        }
        TriggerRuntimeInstance completed = instances.get(instanceId.orElseThrow()).completed();
        instances.put(completed.instanceId(), completed);
        return TriggerResult.of(TriggerResultCode.FIRED, completed.snapshot());
    }

    private TriggerRuntimeInstance newInstance(
            CompiledTriggerDefinition definition,
            TriggerInstanceId instanceId,
            TriggerOwnerId owner,
            long generation
    ) {
        return new TriggerRuntimeInstance(definition, instanceId,
                TriggerInstanceIds.execution(instanceId, generation), owner, 0,
                TriggerStatus.WAITING, io.github.kxyr0.echotrigger.api.status.TriggerOutcome.NONE, generation);
    }
}
