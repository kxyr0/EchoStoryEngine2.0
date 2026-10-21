package io.github.kxyr0.echoeffects.forge;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.core.catalog.InMemoryEffectCatalog;
import io.github.kxyr0.echoeffects.core.contribution.EffectContributionSnapshot;
import io.github.kxyr0.echoeffects.core.runtime.EffectApplyRequest;
import io.github.kxyr0.echoeffects.core.runtime.EffectRuntime;
import io.github.kxyr0.echoeffects.core.runtime.EffectRuntimeResult;
import io.github.kxyr0.echoeffects.core.runtime.EffectRuntimeTimelineEvent;
import io.github.kxyr0.echoeffects.forge.api.ActiveEffectSnapshot;
import io.github.kxyr0.echoeffects.forge.api.EffectDefinitionCatalog;
import io.github.kxyr0.echoeffects.forge.api.EffectQueryService;
import io.github.kxyr0.echoeffects.forge.api.EffectService;
import io.github.kxyr0.echoeffects.forge.api.diagnostics.EffectDiagnosticsView;
import io.github.kxyr0.echoeffects.forge.content.ForgeEffectContentResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.event.TickEvent;

public final class ForgeEffectServices implements EffectService, EffectQueryService,
        EffectDefinitionCatalog, EffectDiagnosticsView {
    private final InMemoryEffectCatalog catalog = new InMemoryEffectCatalog();
    private final EffectRuntime runtime = new EffectRuntime(catalog);
    private final List<EffectDiagnostic> diagnostics = new ArrayList<>();
    private long serverTick;
    private long syncRevision;

    public void accept(ForgeEffectContentResult result) {
        diagnostics.clear();
        diagnostics.addAll(result.diagnostics());
        if (diagnostics.stream().anyMatch(EffectDiagnostic::isError)) {
            return;
        }
        var published = catalog.publish(result.definitions());
        diagnostics.addAll(published.diagnostics());
        if (published.successful()) {
            syncRevision++;
        }
    }

    @Override
    public Optional<ActiveEffectSnapshot> applyToTarget(EffectTargetRef target, EffectId effectId,
            EffectCommandId commandId, double intensity) {
        EffectRuntimeResult result = runtime.apply(new EffectApplyRequest(target, effectId, commandId, intensity),
                serverTick);
        diagnostics.addAll(result.diagnostics());
        return result.view().map(view -> new ActiveEffectSnapshot(view, ++syncRevision));
    }

    @Override
    public boolean removeFromTarget(EffectTargetRef target, EffectId effectId, long fadeOutTicks) {
        boolean removed = runtime.remove(target, effectId.value());
        if (removed) {
            syncRevision++;
        }
        return removed;
    }

    @Override
    public List<ActiveEffectSnapshot> activeEffects(EffectTargetRef target) {
        return runtime.activeViews(target, serverTick).stream()
                .map(view -> new ActiveEffectSnapshot(view, syncRevision))
                .toList();
    }

    @Override
    public boolean isActive(EffectTargetRef target, EffectId effectId) {
        return activeEffects(target).stream().anyMatch(snapshot -> snapshot.view().effectId().equals(effectId));
    }

    @Override
    public Optional<EffectDefinition> find(EffectId id) {
        return catalog.find(id);
    }

    @Override
    public List<EffectDefinition> definitions() {
        return catalog.snapshot().definitions();
    }

    @Override
    public List<EffectDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    public void addDiagnostics(List<EffectDiagnostic> values) {
        diagnostics.addAll(values);
    }

    public EffectContributionSnapshot contributionSnapshot(EffectTargetRef target) {
        return runtime.contributionSnapshot(target, serverTick);
    }

    public List<EffectRuntimeTimelineEvent> onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return List.of();
        }
        serverTick++;
        List<EffectRuntimeTimelineEvent> timelineEvents = runtime.drainTimelineEvents(serverTick);
        runtime.tick(serverTick);
        return timelineEvents;
    }

    public long serverTick() {
        return serverTick;
    }
}
