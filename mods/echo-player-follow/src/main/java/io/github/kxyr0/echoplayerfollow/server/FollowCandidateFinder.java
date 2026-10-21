package io.github.kxyr0.echoplayerfollow.server;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationQueryService;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echoplayerfollow.shared.FollowConstants;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FollowCandidateFinder {
    public FollowSearchResult nearestWithReason(
            MinecraftServer server,
            ServerPlayer player,
            double maxDistance,
            Optional<ActorId> requested
    ) {
        Optional<NpcService> npcService = npcService(server);
        Optional<NavigationQueryService> navigation = navigationService(server);
        if (npcService.isEmpty()) {
            return FollowSearchResult.rejected("no_npc_service");
        }
        if (navigation.isEmpty()) {
            return FollowSearchResult.rejected("no_navigation_service");
        }
        List<NpcActorView> actorList = actors(npcService.orElseThrow(), navigation.orElseThrow(), requested);
        if (actorList.isEmpty()) {
            return FollowSearchResult.rejected(requested.isPresent() ? "actor_missing" : "not_moving");
        }
        List<FollowSearchResult> results = actorList.stream()
                .map(actor -> candidate(server, player, actor, navigation.orElseThrow(), maxDistance))
                .toList();
        return results.stream()
                .flatMap(result -> result.candidate().stream())
                .min(Comparator.comparingDouble(FollowCandidate::distance))
                .map(FollowSearchResult::found)
                .orElseGet(() -> FollowSearchResult.rejected(priorityReason(results)));
    }

    public FollowSearchResult targetedWithReason(
            MinecraftServer server,
            ServerPlayer player,
            double maxDistance,
            int targetEntityId
    ) {
        if (targetEntityId < 0) {
            return nearestWithReason(server, player, maxDistance, Optional.empty());
        }
        Optional<NpcService> npcService = npcService(server);
        Optional<NavigationQueryService> navigation = navigationService(server);
        if (npcService.isEmpty()) {
            return FollowSearchResult.rejected("no_npc_service");
        }
        if (navigation.isEmpty()) {
            return FollowSearchResult.rejected("no_navigation_service");
        }
        List<FollowSearchResult> results = movingActors(npcService.orElseThrow(), navigation.orElseThrow()).stream()
                .map(actor -> candidate(server, player, actor, navigation.orElseThrow(), maxDistance))
                .toList();
        return results.stream()
                .flatMap(result -> result.candidate().stream())
                .filter(candidate -> candidate.entityId() == targetEntityId)
                .findFirst()
                .map(FollowSearchResult::found)
                .orElseGet(() -> FollowSearchResult.rejected(priorityReason(results)));
    }

    private List<NpcActorView> actors(
            NpcService service,
            NavigationQueryService navigation,
            Optional<ActorId> requested
    ) {
        return requested.flatMap(id -> service.query(id).actor())
                .map(List::of)
                .orElseGet(() -> movingActors(service, navigation));
    }

    private List<NpcActorView> movingActors(NpcService service, NavigationQueryService navigation) {
        return navigation.list(FollowConstants.NAVIGATION_SCAN_LIMIT).stream()
                .filter(snapshot -> activeMovement(snapshot.status()))
                .map(NavigationSnapshot::subjectId)
                .map(subject -> actorId(subject.value()))
                .flatMap(Optional::stream)
                .distinct()
                .map(id -> service.query(id).actor())
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ActorId> actorId(String value) {
        try {
            return Optional.of(new ActorId(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private FollowSearchResult candidate(
            MinecraftServer server,
            ServerPlayer player,
            NpcActorView actor,
            NavigationQueryService navigation,
            double maxDistance
    ) {
        if (!materialized(actor)) {
            return FollowSearchResult.rejected("not_materialized");
        }
        if (!sameDimension(player, actor)) {
            return FollowSearchResult.rejected("different_dimension");
        }
        if (!moving(navigation, actor.actorId())) {
            return FollowSearchResult.rejected("not_moving");
        }
        Optional<NpcNavigationBody> npcBody = body(server, actor.actorId());
        if (npcBody.isEmpty()) {
            return FollowSearchResult.rejected("no_body");
        }
        NpcNavigationBody value = npcBody.orElseThrow();
        if (!value.mob().isAlive() || value.mob().isRemoved()) {
            return FollowSearchResult.rejected("body_dead");
        }
        if (!value.dimensionId().equals(player.serverLevel().dimension().location().toString())) {
            return FollowSearchResult.rejected("body_dimension");
        }
        return toCandidate(player, actor, value, maxDistance);
    }

    private FollowSearchResult toCandidate(
            ServerPlayer player,
            NpcActorView actor,
            NpcNavigationBody body,
            double maxDistance
    ) {
        double distance = player.distanceTo(body.mob());
        if (distance > maxDistance) {
            return FollowSearchResult.rejected("too_far");
        }
        return FollowSearchResult.found(new FollowCandidate(actor.actorId(), actor.displayName(),
                body.mob().getId(), distance));
    }

    private String priorityReason(List<FollowSearchResult> results) {
        for (String reason : List.of("too_far", "body_dimension", "body_dead", "no_body", "not_moving",
                "different_dimension", "not_materialized")) {
            if (results.stream().anyMatch(result -> reason.equals(result.reason()))) {
                return reason;
            }
        }
        return "no_candidate";
    }

    private Optional<NpcNavigationBody> body(MinecraftServer server, ActorId actorId) {
        return EchoNpcNavigationBodyBridge.lookup(server, actorId).body();
    }

    private boolean materialized(NpcActorView actor) {
        return actor.status() == NpcActorView.Status.MATERIALIZED && actor.materializedPosition().isPresent();
    }

    private boolean sameDimension(ServerPlayer player, NpcActorView actor) {
        return actor.worldId().value().equals(player.serverLevel().dimension().location().toString());
    }

    private boolean moving(NavigationQueryService service, ActorId actorId) {
        return service.active(new NavigationSubjectId(actorId.value()))
                .map(snapshot -> activeMovement(snapshot.status()))
                .orElse(false);
    }

    private boolean activeMovement(NavigationStatus status) {
        return status == NavigationStatus.VALIDATING
                || status == NavigationStatus.PLANNING
                || status == NavigationStatus.MOVING
                || status == NavigationStatus.REPLANNING;
    }

    private Optional<NpcService> npcService(MinecraftServer server) {
        return EchoNpcServiceBridge.lookup(server).service();
    }

    private Optional<NavigationQueryService> navigationService(MinecraftServer server) {
        return EchoNavigationServiceBridge.lookup(server).queryService();
    }
}
