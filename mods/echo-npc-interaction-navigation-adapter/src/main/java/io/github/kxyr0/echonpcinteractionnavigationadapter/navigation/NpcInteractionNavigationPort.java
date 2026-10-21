package io.github.kxyr0.echonpcinteractionnavigationadapter.navigation;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NavigationDispatch;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NavigationObservation;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class NpcInteractionNavigationPort implements NpcInteractionExecutionPort {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ApproachWorldChecks checks = new ApproachWorldChecks();

    @Override
    public String portId() {
        return "echo_npc_interaction_navigation_adapter:navigation";
    }

    @Override
    public Optional<NpcInteractionApproach> selectApproach(
            MinecraftServer server,
            NpcInteractionRequest request,
            NpcInteractionPlan plan) {
        var body = EchoNpcNavigationBodyBridge.lookup(server, request.npcId()).body();
        if (body.isEmpty()) {
            LOGGER.warn("NPC_INTERACTION_APPROACH_BODY_MISSING npc={} commandId={}",
                    request.npcId().value(), request.commandId().value());
            return Optional.empty();
        }
        Optional<NpcInteractionApproach> selected = plan.approaches().stream()
                .sorted(Comparator.comparingDouble(NpcInteractionApproach::estimatedApproachCost)
                        .thenComparingDouble(approach -> distanceTo(body.orElseThrow(), approach)))
                .filter(approach -> checks.usable(body.orElseThrow(), request, approach))
                .findFirst();
        if (selected.isEmpty()) {
            logRejectedApproaches(body.orElseThrow(), request, plan);
        }
        return selected;
    }

    @Override
    public NavigationDispatch startNavigation(MinecraftServer server, NpcInteractionRequest request,
            NpcInteractionApproach approach) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.service().isEmpty() || lookup.queryService().isEmpty()) {
            return new NavigationDispatch(NavigationDispatch.Status.UNAVAILABLE, Optional.empty(),
                    "Echo Navigation is unavailable.");
        }
        var settings = lookup.queryService().orElseThrow().settings();
        NavigationRequest navigation = new NavigationRequest(NpcInteractionNavigationIds.move(request.commandId()),
                NpcInteractionNavigationIds.command(request.commandId()),
                NpcInteractionNavigationIds.execution(request.commandId()),
                NpcInteractionNavigationIds.owner(request.commandId()),
                NpcInteractionNavigationIds.subject(request.npcId()), destination(request, approach),
                settings.backendId(), settings.revision(), 0L);
        var result = lookup.service().orElseThrow().move(navigation);
        return new NavigationDispatch(dispatchStatus(result.status()), result.snapshot().map(value -> value.moveId()),
                result.message());
    }

    @Override
    public Optional<NavigationObservation> navigation(MinecraftServer server, NavigationMoveId moveId) {
        return EchoNavigationServiceBridge.lookup(server).queryService()
                .flatMap(query -> query.move(moveId))
                .map(snapshot -> new NavigationObservation(moveId, snapshot.status().name(),
                        snapshot.status().terminal(), snapshot.status().name().equals("ARRIVED"),
                        snapshot.status().name()));
    }

    @Override
    public NavigationDispatch cancelNavigation(MinecraftServer server, NavigationMoveId moveId) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) {
            return new NavigationDispatch(NavigationDispatch.Status.UNAVAILABLE, Optional.empty(),
                    "Echo Navigation is unavailable.");
        }
        var result = lookup.service().orElseThrow().cancel(moveId);
        return new NavigationDispatch(dispatchStatus(result.status()), Optional.of(moveId), result.message());
    }

    @Override
    public List<String> approachDiagnostics(MinecraftServer server, NpcInteractionRequest request,
            NpcInteractionPlan plan) {
        var body = EchoNpcNavigationBodyBridge.lookup(server, request.npcId()).body();
        if (body.isEmpty()) {
            return List.of("NPC_INTERACTION_APPROACH_BODY_MISSING npc=" + request.npcId().value());
        }
        java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger();
        return plan.approaches().stream().map(approach -> diagnostic(index.incrementAndGet(),
                body.orElseThrow(), request, approach)).toList();
    }

    private NavigationDestination destination(NpcInteractionRequest request, NpcInteractionApproach approach) {
        return new NavigationDestination(request.target().dimensionId(),
                (int) Math.floor(approach.standingPosition().x()),
                (int) Math.floor(approach.standingPosition().y()),
                (int) Math.floor(approach.standingPosition().z()));
    }

    private double distanceTo(NpcNavigationBody body, NpcInteractionApproach approach) {
        double dx = body.mob().getX() - approach.standingPosition().x();
        double dy = body.mob().getY() - approach.standingPosition().y();
        double dz = body.mob().getZ() - approach.standingPosition().z();
        return dx * dx + dy * dy + dz * dz;
    }

    private void logRejectedApproaches(NpcNavigationBody body, NpcInteractionRequest request,
            NpcInteractionPlan plan) {
        for (NpcInteractionApproach approach : plan.approaches()) {
            LOGGER.info("NPC_INTERACTION_APPROACH_REJECTED commandId={} npc={} standing={} face={} reason={}",
                    request.commandId().value(), request.npcId().value(), approach.standingPosition(),
                    approach.hit().blockFace(), checks.rejectionReason(body, request, approach));
        }
    }

    private String diagnostic(int id, NpcNavigationBody body, NpcInteractionRequest request,
            NpcInteractionApproach approach) {
        return "candidate=" + id
                + " standing=" + approach.standingPosition()
                + " facing=" + approach.facingDirection()
                + " hit=" + approach.hit().hitPoint()
                + " face=" + approach.hit().blockFace()
                + " requiredReach=" + approach.requiredReach()
                + " score=" + approach.estimatedApproachCost()
                + " rejection=" + checks.rejectionReason(body, request, approach);
    }

    private NavigationDispatch.Status dispatchStatus(
            io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status status) {
        return switch (status) {
            case ACCEPTED, WAITING -> NavigationDispatch.Status.ACCEPTED;
            case DUPLICATE -> NavigationDispatch.Status.DUPLICATE;
            case CONFLICT, REJECTED, FAILED -> NavigationDispatch.Status.REJECTED;
            case UNAVAILABLE -> NavigationDispatch.Status.UNAVAILABLE;
        };
    }
}
