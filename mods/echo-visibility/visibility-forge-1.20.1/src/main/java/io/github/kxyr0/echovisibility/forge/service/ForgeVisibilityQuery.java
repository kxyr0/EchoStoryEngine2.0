package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.forge.api.VisibilityQueryService;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchCatalog;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchView;
import io.github.kxyr0.echovisibility.forge.api.concealment.VisibilityConcealmentSnapshot;
import io.github.kxyr0.echovisibility.forge.api.concealment.VisibilityConcealmentView;
import io.github.kxyr0.echovisibility.forge.api.diagnostic.VisibilityDiagnosticsView;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeVisibilityQuery implements VisibilityQueryService, VisibilityWatchCatalog,
        VisibilityConcealmentView, VisibilityDiagnosticsView {
    private final VisibilityServerState state;

    public ForgeVisibilityQuery(VisibilityServerState state) {
        this.state = state;
    }

    @Override
    public Optional<VisibilityWatchView> find(ServerPlayer player, VisibilityWatchId watchId) {
        return state.arms().find(VisibilityServerState.player(player), watchId)
                .map(value -> VisibilityWatchViews.from(value.snapshot()));
    }

    @Override
    public List<VisibilityWatchView> active(ServerPlayer player) {
        return state.arms().active(VisibilityServerState.player(player)).stream()
                .map(VisibilityWatchViews::from).toList();
    }

    @Override
    public boolean isVisible(ServerPlayer player, VisibilityWatchId watchId) {
        return find(player, watchId).map(value -> value.state().visible()).orElse(false);
    }

    @Override
    public boolean isOutOfView(ServerPlayer player, VisibilityWatchId watchId) {
        return find(player, watchId).map(value -> value.state().geometricallyHidden()).orElse(false);
    }

    @Override
    public Optional<VisibilityWatchDefinition> find(VisibilityWatchId watchId) {
        return state.catalog().find(watchId);
    }

    @Override
    public List<VisibilityWatchDefinition> all() {
        return state.catalog().all();
    }

    @Override
    public long catalogRevision() {
        return state.catalog().catalogRevision();
    }

    @Override
    public List<VisibilityConcealmentSnapshot> leases(ServerPlayer player) {
        return state.leases(VisibilityServerState.player(player)).stream()
                .filter(value -> value.status()
                        == io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus.PENDING
                        || value.status()
                        == io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus.CLAIMED)
                .map(value -> new VisibilityConcealmentSnapshot(value.id(), value.watchId(), value.generation(),
                        value.npcId(), value.status(), value.deadlineTick())).toList();
    }

    @Override
    public List<VisibilityDiagnostic> recent(Optional<VisibilityWatchId> watchId, int limit) {
        return state.diagnostics().recent(watchId, limit);
    }
}
