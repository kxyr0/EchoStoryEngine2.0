package io.github.kxyr0.echoplayeractor.forge.movement;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMotor;
import io.github.kxyr0.echoplayeractor.forge.body.PlayerActorSnapshots;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class ServerPlayerActorMotor implements PlayerActorMotor {
    private static final double STEP = 0.14D;
    private static final double ARRIVAL = 0.18D;
    private final ServerPlayerBodyResolver resolver;
    private final PlayerActorSnapshots snapshots;
    private final Map<PlayerActorId, ActiveMove> moves = new HashMap<>();
    private long revision;

    public ServerPlayerActorMotor(ServerPlayerBodyResolver resolver, PlayerActorSnapshots snapshots) {
        this.resolver = resolver;
        this.snapshots = snapshots;
    }

    @Override
    public PlayerActorResult start(PlayerActorMoveRequest request) {
        Optional<ServerPlayer> player = resolver.resolve(request.actorId());
        if (player.isEmpty()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE, "Player actor is offline.");
        }
        if (player.orElseThrow().isPassenger()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_STILL_SEATED,
                    "Player.Move requires a standing actor.");
        }
        if (!snapshots.dimensionId(player.orElseThrow()).equals(request.dimensionId())) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_MOVE_DESTINATION_INVALID,
                    "Cross-dimension Player.Move is outside PA1.");
        }
        moves.put(request.actorId(), new ActiveMove(request, player.orElseThrow().getX(),
                player.orElseThrow().getY(), player.orElseThrow().getZ()));
        return PlayerActorResult.waiting(PlayerActorOutcome.MOVING,
                snapshot(player.orElseThrow(), Optional.of(request.commandId())), "Player move started.");
    }

    @Override
    public PlayerActorResult stop(PlayerActorId actorId) {
        moves.remove(actorId);
        Optional<ServerPlayer> player = resolver.resolve(actorId);
        return player.map(value -> PlayerActorResult.success(PlayerActorOutcome.STOPPED,
                snapshot(value, Optional.empty()), "Player move stopped."))
                .orElseGet(() -> PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE,
                        "Player actor is offline."));
    }

    @Override
    public boolean isMoving(PlayerActorId actorId) {
        return moves.containsKey(actorId);
    }

    public Optional<PlayerActorCommandId> activeCommand(PlayerActorId actorId) {
        return Optional.ofNullable(moves.get(actorId)).map(ActiveMove::request).map(PlayerActorMoveRequest::commandId);
    }

    public void tick() {
        Iterator<Map.Entry<PlayerActorId, ActiveMove>> iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveMove move = iterator.next().getValue();
            Optional<ServerPlayer> player = resolver.resolve(move.request().actorId());
            if (player.isEmpty() || tickOne(move, player.orElseThrow())) {
                iterator.remove();
            }
        }
    }

    private boolean tickOne(ActiveMove move, ServerPlayer player) {
        if (move.advance() > move.request().timeoutTicks()) {
            return true;
        }
        Vec3 current = player.position();
        Vec3 target = new Vec3(move.request().destination().x(), move.request().destination().y(),
                move.request().destination().z());
        Vec3 delta = target.subtract(current);
        double distance = delta.length();
        if (distance <= ARRIVAL) {
            snapshot(player, Optional.empty());
            return true;
        }
        Vec3 step = delta.normalize().scale(Math.min(STEP, distance));
        player.move(MoverType.SELF, step);
        snapshot(player, Optional.of(move.request().commandId()));
        return move.updateStuck(player.getX(), player.getY(), player.getZ()) > 20;
    }

    private io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot snapshot(
            ServerPlayer player,
            Optional<PlayerActorCommandId> activeCommand
    ) {
        revision += 1L;
        return snapshots.snapshot(player, activeCommand.isPresent(), activeCommand, revision);
    }
}
