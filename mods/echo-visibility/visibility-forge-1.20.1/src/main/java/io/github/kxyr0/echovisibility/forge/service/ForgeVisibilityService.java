package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.core.arm.VisibilityArmResult;
import io.github.kxyr0.echovisibility.forge.api.VisibilityService;
import io.github.kxyr0.echovisibility.forge.api.VisibilityServiceResult;
import io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolution;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchView;
import io.github.kxyr0.echovisibility.forge.network.ClientboundArmPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundConcealmentPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundDisarmPacket;
import io.github.kxyr0.echovisibility.forge.network.EchoVisibilityNetwork;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeVisibilityService implements VisibilityService {
    private static final long BLIND_WINDOW_FRESHNESS_TICKS = 2L;
    private final VisibilityServerState state;

    public ForgeVisibilityService(VisibilityServerState state) {
        this.state = state;
    }

    @Override
    public VisibilityServiceResult arm(ServerPlayer player, VisibilityWatchId watchId, String ownerLineage) {
        var definition = state.catalog().find(watchId);
        if (definition.isEmpty()) {
            return failure(VisibilityServiceResult.Status.NOT_FOUND,
                    VisibilityDiagnosticCode.VISIBILITY_WATCH_NOT_FOUND, "Visibility watch not found: " + watchId);
        }
        VisibilityArmResult result = state.arms().arm(VisibilityServerState.player(player),
                definition.orElseThrow(), ownerLineage, state.server().getTickCount(), state.connection(player));
        if (result.status() != VisibilityArmResult.Status.ARMED) {
            result.diagnostic().ifPresent(state.diagnostics()::add);
            return new VisibilityServiceResult(map(result.status()), Optional.empty(), result.diagnostic());
        }
        var armed = state.arms().find(VisibilityServerState.player(player), watchId).orElseThrow();
        VisibilityTargetResolution target = state.resolve(player, armed);
        if (target.status() != VisibilityTargetResolution.Status.RESOLVED) {
            state.arms().disarm(VisibilityServerState.player(player), watchId);
            return new VisibilityServiceResult(VisibilityServiceResult.Status.FAILED,
                    Optional.empty(), target.diagnostic());
        }
        EchoVisibilityNetwork.send(player, new ClientboundArmPacket(armed.definition(), armed.generation(),
                armed.connectionGeneration(), target.bounds(), target.clientEntityId().orElse(-1)));
        state.markDirty();
        return success(VisibilityServiceResult.Status.ARMED, armed.snapshot());
    }

    @Override
    public VisibilityServiceResult disarm(ServerPlayer player, VisibilityWatchId watchId) {
        VisibilityArmResult result = state.arms().disarm(VisibilityServerState.player(player), watchId);
        if (result.status() != VisibilityArmResult.Status.DISARMED) {
            result.diagnostic().ifPresent(state.diagnostics()::add);
            return new VisibilityServiceResult(VisibilityServiceResult.Status.NOT_ARMED,
                    Optional.empty(), result.diagnostic());
        }
        var snapshot = result.arm().orElseThrow();
        EchoVisibilityNetwork.send(player, new ClientboundDisarmPacket(watchId, snapshot.generation()));
        state.markDirty();
        return success(VisibilityServiceResult.Status.DISARMED, snapshot);
    }

    @Override
    public VisibilityServiceResult assertStillOutOfView(ServerPlayer player, VisibilityWatchId watchId) {
        var arm = state.arms().find(VisibilityServerState.player(player), watchId);
        if (arm.isEmpty()) {
            return failure(VisibilityServiceResult.Status.NOT_ARMED,
                    VisibilityDiagnosticCode.VISIBILITY_WATCH_NOT_ARMED, "Visibility watch is not armed.");
        }
        var snapshot = arm.orElseThrow().snapshot();
        long age = state.server().getTickCount() - snapshot.lastAcceptedServerTick();
        if (!snapshot.lastState().geometricallyHidden() || snapshot.lastAcceptedServerTick() < 0L
                || age > BLIND_WINDOW_FRESHNESS_TICKS) {
            return failure(VisibilityServiceResult.Status.BLIND_WINDOW_CLOSED,
                    VisibilityDiagnosticCode.VISIBILITY_BLIND_WINDOW_CLOSED,
                    "Visibility blind window is closed or stale.");
        }
        return success(VisibilityServiceResult.Status.ASSERTED, snapshot);
    }

    @Override
    public VisibilityServiceResult claimConcealment(
            ServerPlayer player,
            VisibilityConcealmentLeaseId leaseId,
            String claimantId) {
        return updateLease(player, leaseId, VisibilityServiceResult.Status.CLAIMED, claimantId, 0);
    }

    @Override
    public VisibilityServiceResult commitConcealment(
            ServerPlayer player,
            VisibilityConcealmentLeaseId leaseId) {
        return updateLease(player, leaseId, VisibilityServiceResult.Status.COMMITTED, "committed", 1);
    }

    @Override
    public VisibilityServiceResult rejectConcealment(
            ServerPlayer player,
            VisibilityConcealmentLeaseId leaseId) {
        return updateLease(player, leaseId, VisibilityServiceResult.Status.REJECTED, "rejected", 2);
    }

    private VisibilityServiceResult updateLease(
            ServerPlayer player,
            VisibilityConcealmentLeaseId leaseId,
            VisibilityServiceResult.Status resultStatus,
            String message,
            int operation) {
        var lease = state.lease(VisibilityServerState.player(player), leaseId);
        boolean changed = lease.filter(value -> switch (operation) {
            case 0 -> value.claim();
            case 1 -> value.commit();
            default -> value.reject();
        }).isPresent();
        if (!changed) {
            return failure(VisibilityServiceResult.Status.FAILED,
                    VisibilityDiagnosticCode.VISIBILITY_CONCEALMENT_REJECTED,
                    "Concealment lease cannot perform operation: " + message);
        }
        var snapshot = lease.orElseThrow().snapshot();
        EchoVisibilityNetwork.send(player,
                new ClientboundConcealmentPacket(leaseId, snapshot.status(), message));
        var arm = state.arms().find(VisibilityServerState.player(player), snapshot.watchId());
        return arm.map(value -> success(resultStatus, value.snapshot())).orElseGet(() ->
                new VisibilityServiceResult(resultStatus, Optional.empty(), Optional.empty()));
    }

    private VisibilityServiceResult failure(
            VisibilityServiceResult.Status status,
            VisibilityDiagnosticCode code,
            String message) {
        VisibilityDiagnostic diagnostic = VisibilityDiagnostic.of(code, message);
        state.diagnostics().add(diagnostic);
        return new VisibilityServiceResult(status, Optional.empty(), Optional.of(diagnostic));
    }

    private static VisibilityServiceResult success(
            VisibilityServiceResult.Status status,
            io.github.kxyr0.echovisibility.core.arm.VisibilityArmSnapshot snapshot) {
        VisibilityWatchView view = VisibilityWatchViews.from(snapshot);
        return new VisibilityServiceResult(status, Optional.of(view), Optional.empty());
    }

    private static VisibilityServiceResult.Status map(VisibilityArmResult.Status status) {
        return switch (status) {
            case LIMIT_REACHED -> VisibilityServiceResult.Status.LIMIT_REACHED;
            case NOT_ARMED -> VisibilityServiceResult.Status.NOT_ARMED;
            case ALREADY_ARMED -> VisibilityServiceResult.Status.REJECTED;
            default -> VisibilityServiceResult.Status.FAILED;
        };
    }
}
