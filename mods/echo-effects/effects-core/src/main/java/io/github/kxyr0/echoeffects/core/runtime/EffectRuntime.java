package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.identity.EffectInstanceId;
import io.github.kxyr0.echoeffects.api.status.EffectOutcome;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.api.view.ActiveEffectView;
import io.github.kxyr0.echoeffects.core.catalog.InMemoryEffectCatalog;
import io.github.kxyr0.echoeffects.core.contribution.EffectContributionComposer;
import io.github.kxyr0.echoeffects.core.contribution.EffectContributionSnapshot;
import io.github.kxyr0.echoeffects.core.curve.EffectCurveSampler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EffectRuntime {
    private final InMemoryEffectCatalog catalog;
    private final EffectCurveSampler sampler = new EffectCurveSampler();
    private final EffectContributionComposer contributions = new EffectContributionComposer();
    private final Map<EffectTargetRef, List<ActiveEffectInstance>> active = new LinkedHashMap<>();

    public EffectRuntime(InMemoryEffectCatalog catalog) {
        this.catalog = catalog;
    }

    public EffectRuntimeResult apply(EffectApplyRequest request, long serverTick) {
        return catalog.find(request.effectId())
                .map(definition -> applyKnown(request, serverTick))
                .orElseGet(() -> missingDefinition(request));
    }

    public boolean remove(EffectTargetRef target, String effectId) {
        List<ActiveEffectInstance> instances = active.getOrDefault(target, List.of());
        instances.stream()
                .filter(instance -> instance.definition().id().value().equals(effectId))
                .forEach(instance -> instance.remove(EffectOutcome.REMOVED));
        return instances.stream().anyMatch(ActiveEffectInstance::terminal);
    }

    public void tick(long serverTick) {
        active.values().forEach(instances -> instances.forEach(instance -> instance.tick(serverTick)));
        active.values().forEach(instances -> instances.removeIf(ActiveEffectInstance::terminal));
        active.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public List<EffectRuntimeTimelineEvent> drainTimelineEvents(long serverTick) {
        List<EffectRuntimeTimelineEvent> result = new ArrayList<>();
        active.values().forEach(instances -> instances.forEach(instance ->
                instance.drainTimelineEvents(serverTick).forEach(event ->
                        result.add(new EffectRuntimeTimelineEvent(instance.target(), instance.instanceId(), event)))));
        return List.copyOf(result);
    }

    public List<ActiveEffectView> activeViews(EffectTargetRef target, long serverTick) {
        return active.getOrDefault(target, List.of()).stream()
                .filter(instance -> !instance.terminal())
                .map(instance -> instance.view(serverTick))
                .toList();
    }

    public EffectContributionSnapshot contributionSnapshot(EffectTargetRef target, long serverTick) {
        List<ActiveEffectInstance> instances = active.getOrDefault(target, List.of());
        return contributions.compose(instances.stream()
                .filter(instance -> !instance.terminal())
                .flatMap(instance -> instance.contributions(serverTick, sampler).stream())
                .toList());
    }

    private EffectRuntimeResult applyKnown(EffectApplyRequest request, long serverTick) {
        List<ActiveEffectInstance> instances = active.computeIfAbsent(request.target(), ignored -> new ArrayList<>());
        Optional<ActiveEffectInstance> existing = instances.stream()
                .filter(instance -> instance.definition().id().equals(request.effectId()))
                .findFirst();
        if (existing.isPresent() && requestPolicy(existing.orElseThrow(), request, serverTick)) {
            return new EffectRuntimeResult(Optional.of(existing.orElseThrow().view(serverTick)), List.of());
        }
        ActiveEffectInstance instance = start(request, serverTick, instances.size() + 1);
        instances.add(instance);
        return new EffectRuntimeResult(Optional.of(instance.view(serverTick)), List.of());
    }

    private boolean requestPolicy(ActiveEffectInstance existing, EffectApplyRequest request, long serverTick) {
        return switch (existing.definition().stackingPolicy()) {
            case IGNORE_NEW -> true;
            case REFRESH_DURATION -> {
                existing.refresh(serverTick);
                yield true;
            }
            case STACK_INTENSITY -> {
                existing.stackIntensity(request.intensity());
                yield true;
            }
            case RESTART, REPLACE_OLD -> {
                existing.remove(EffectOutcome.CANCELLED);
                yield false;
            }
            case ALLOW_MULTIPLE -> false;
        };
    }

    private ActiveEffectInstance start(EffectApplyRequest request, long serverTick, int generation) {
        EffectInstanceId instanceId = EffectInstanceId.deterministic(request.commandId(), generation);
        return new ActiveEffectInstance(instanceId, catalog.find(request.effectId()).orElseThrow(),
                request.target(), serverTick, request.intensity());
    }

    private EffectRuntimeResult missingDefinition(EffectApplyRequest request) {
        EffectDiagnostic diagnostic = EffectDiagnostic.error(EffectDiagnosticCode.EFFECT_DEFINITION_MISSING,
                "Effect definition is not loaded: " + request.effectId().value(), request.effectId().value());
        return new EffectRuntimeResult(Optional.empty(), List.of(diagnostic));
    }
}
