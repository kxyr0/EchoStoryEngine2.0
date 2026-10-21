package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

public final class MovementCommandAdapter {
    private static final int TIMEOUT_TICKS = 1_200;
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final LocationCatalog locations;
    private final Map<String, Pending> pending = new LinkedHashMap<>();

    MovementCommandAdapter(Function<SceneExecutionId, ServerPlayer> players, LocationCatalog locations) {
        this.players = players;
        this.locations = locations;
    }

    SceneCommandStart start(SceneCommandRequest request) {
        SceneLocation location = locations.find(request.command().arguments().get(0))
                .orElseThrow(() -> new IllegalArgumentException("Scene location not found"));
        return SceneCommandHub.playerCommand(request) ? movePlayer(request, location) : moveNpc(request, location);
    }

    void tick(SceneCommandHub.Completion completion) {
        for (Pending value : List.copyOf(pending.values())) {
            Outcome outcome = value.playerMove() ? playerOutcome(value) : npcOutcome(value);
            if (outcome.terminal()) finish(value, outcome.succeeded(), outcome.detail(), completion);
            else if (value.remainingTicks() <= 1) finish(value, false, "movement timed out", completion);
            else pending.put(value.key(), value.tick());
        }
    }

    void cancelOwned(SceneExecutionId owner) {
        pending.values().removeIf(value -> {
            if (!value.execution().equals(owner)) return false;
            if (value.playerMove()) {
                var lookup = EchoPlayerActorServiceBridge.lookup(value.player().getServer());
                lookup.service().ifPresent(service -> service.stop(PlayerActorId.of(value.player().getUUID()),
                        playerOwner(value.rootOwner())));
            } else {
                var lookup = EchoNavigationServiceBridge.lookup(value.player().getServer());
                lookup.service().ifPresent(service -> service.cancel(new NavigationMoveId(value.key())));
            }
            return true;
        });
    }

    private SceneCommandStart movePlayer(SceneCommandRequest request, SceneLocation location) {
        ServerPlayer player = requirePlayer(request);
        var lookup = EchoPlayerActorServiceBridge.lookup(player.getServer());
        var service = lookup.service().orElseThrow(() -> new IllegalStateException("Player Actor unavailable"));
        String key = SceneCommandHub.key(request);
        var result = service.move(new PlayerActorMoveRequest(new PlayerActorCommandId(key),
                PlayerActorId.of(player.getUUID()), playerOwner(request.rootOwner()),
                new Vector3(location.x(), location.y(), location.z()), location.dimension(), TIMEOUT_TICKS));
        if (result.status() == PlayerActorStatus.SUCCEEDED) return SceneCommandStart.completed();
        if (result.status() == PlayerActorStatus.ACCEPTED || result.status() == PlayerActorStatus.WAITING
                || result.status() == PlayerActorStatus.RUNNING || result.status() == PlayerActorStatus.DUPLICATE) {
            pending.put(key, Pending.player(request, key, location, player));
            return SceneCommandStart.waiting(key);
        }
        return SceneCommandStart.failed(result.message());
    }

    private SceneCommandStart moveNpc(SceneCommandRequest request, SceneLocation location) {
        ServerPlayer player = requirePlayer(request);
        var lookup = EchoNavigationServiceBridge.lookup(player.getServer());
        var service = lookup.service().orElseThrow(() -> new IllegalStateException("Navigation unavailable"));
        var query = lookup.queryService().orElseThrow();
        String key = SceneCommandHub.key(request);
        NavigationRequest move = new NavigationRequest(new NavigationMoveId(key), new NavigationCommandId(key),
                new NavigationExecutionId("echo_scene:" + SceneCommandHub.token(request.executionId().value())),
                new NavigationOwnerId("echo_scene:" + SceneCommandHub.token(request.rootOwner())),
                new NavigationSubjectId(request.command().receiver()), destination(location),
                query.settings().backendId(), query.settings().revision(), 0L);
        io.github.kxyr0.echoscene.forge.command.context.SceneEventContexts.capture(request, player,
                move.moveId().value(), move.executionId().value());
        var result = service.move(move);
        if (result.snapshot().filter(value -> value.status() == NavigationStatus.ARRIVED).isPresent()) {
            return SceneCommandStart.completed();
        }
        if (result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.ACCEPTED
                || result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.WAITING
                || result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.DUPLICATE) {
            pending.put(key, Pending.npc(request, key, location, player));
            return SceneCommandStart.waiting(key);
        }
        return SceneCommandStart.failed(result.message());
    }

    private Outcome playerOutcome(Pending value) {
        var lookup = EchoPlayerActorServiceBridge.lookup(value.player().getServer());
        boolean moving = lookup.queryService().map(service -> service.isMoving(PlayerActorId.of(value.player()
                .getUUID()))).orElse(false);
        if (moving) return Outcome.waiting();
        boolean arrived = value.player().serverLevel().dimension().location().toString()
                .equals(value.location().dimension()) && value.player().distanceToSqr(value.location().x(),
                value.location().y(), value.location().z()) <= 0.25D;
        return arrived ? Outcome.success() : Outcome.failure("Player movement stopped before destination");
    }

    private Outcome npcOutcome(Pending value) {
        var lookup = EchoNavigationServiceBridge.lookup(value.player().getServer());
        var snapshot = lookup.queryService().flatMap(service -> service.move(new NavigationMoveId(value.key())));
        if (snapshot.isEmpty() || !snapshot.orElseThrow().status().terminal()) return Outcome.waiting();
        return snapshot.orElseThrow().status() == NavigationStatus.ARRIVED ? Outcome.success()
                : Outcome.failure("NPC movement ended with " + snapshot.orElseThrow().status());
    }

    private void finish(Pending value, boolean success, String detail, SceneCommandHub.Completion completion) {
        pending.remove(value.key());
        completion.complete(value.execution(), value.step(), success, detail);
    }

    private ServerPlayer requirePlayer(SceneCommandRequest request) {
        return Optional.ofNullable(players.apply(request.executionId()))
                .orElseThrow(() -> new IllegalStateException("player unavailable"));
    }
    private NavigationDestination destination(SceneLocation value) {
        return new NavigationDestination(value.dimension(), (int) Math.floor(value.x()),
                (int) Math.floor(value.y()), (int) Math.floor(value.z()));
    }
    private PlayerActorOwnerId playerOwner(String rootOwner) {
        return PlayerActorOwnerId.system("echo_scene_" + SceneCommandHub.token(rootOwner));
    }

    @FunctionalInterface
    public interface LocationCatalog { Optional<SceneLocation> find(String resourceId); }
    public record SceneLocation(String id, String dimension, double x, double y, double z) { }
    private record Pending(String key, SceneExecutionId execution, SceneStepId step, String rootOwner,
                           SceneLocation location, ServerPlayer player, boolean playerMove, int remainingTicks) {
        static Pending player(SceneCommandRequest request, String key, SceneLocation location, ServerPlayer player) {
            return create(request, key, location, player, true);
        }
        static Pending npc(SceneCommandRequest request, String key, SceneLocation location, ServerPlayer player) {
            return create(request, key, location, player, false);
        }
        private static Pending create(SceneCommandRequest request, String key, SceneLocation location,
                ServerPlayer player, boolean playerMove) {
            return new Pending(key, request.executionId(), request.command().stepId(), request.rootOwner(),
                    location, player, playerMove, TIMEOUT_TICKS);
        }
        Pending tick() { return new Pending(key, execution, step, rootOwner, location, player, playerMove,
                remainingTicks - 1); }
    }
    private record Outcome(boolean terminal, boolean succeeded, String detail) {
        static Outcome waiting() { return new Outcome(false, false, ""); }
        static Outcome success() { return new Outcome(true, true, ""); }
        static Outcome failure(String detail) { return new Outcome(true, false, detail); }
    }
}
