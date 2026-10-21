package io.github.kxyr0.echoplayeractor.forge.rotation;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import io.github.kxyr0.echoplayeractor.core.rotation.PlayerActorRotationSample;
import io.github.kxyr0.echoplayeractor.core.rotation.PlayerRotationProgress;
import io.github.kxyr0.echoplayeractor.forge.body.PlayerActorSnapshots;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayerRotationFlow {
    private final ServerPlayerBodyResolver resolver;
    private final PlayerActorSnapshots snapshots;
    private final PlayerRotationProgress progress = new PlayerRotationProgress();
    private final Map<PlayerActorId, ActiveRotation> rotations = new HashMap<>();
    private long revision;

    public ServerPlayerRotationFlow(ServerPlayerBodyResolver resolver, PlayerActorSnapshots snapshots) {
        this.resolver = resolver;
        this.snapshots = snapshots;
    }

    public PlayerActorResult start(PlayerActorRotationRequest request, boolean moving) {
        Optional<ServerPlayer> player = resolver.resolve(request.actorId());
        if (player.isEmpty()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE, "Player actor is offline.");
        }
        if (request.durationTicks() <= 0) {
            apply(player.orElseThrow(), request.yaw(), request.pitch());
            return PlayerActorResult.success(PlayerActorOutcome.ROTATED,
                    snapshot(player.orElseThrow(), moving, Optional.empty()), "Player rotation applied.");
        }
        rotations.put(request.actorId(), new ActiveRotation(request, player.orElseThrow().getYRot(),
                player.orElseThrow().getXRot()));
        return PlayerActorResult.waiting(PlayerActorOutcome.NONE,
                snapshot(player.orElseThrow(), moving, Optional.of(request.commandId())), "Player rotation started.");
    }

    public void tick(boolean moving) {
        Iterator<Map.Entry<PlayerActorId, ActiveRotation>> iterator = rotations.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveRotation active = iterator.next().getValue();
            Optional<ServerPlayer> player = resolver.resolve(active.request().actorId());
            if (player.isEmpty() || tickOne(active, player.orElseThrow(), moving)) {
                iterator.remove();
            }
        }
    }

    public Optional<PlayerActorCommandId> activeCommand(PlayerActorId actorId) {
        return Optional.ofNullable(rotations.get(actorId)).map(ActiveRotation::request)
                .map(PlayerActorRotationRequest::commandId);
    }

    private boolean tickOne(ActiveRotation active, ServerPlayer player, boolean moving) {
        PlayerActorRotationRequest request = active.request();
        int elapsed = active.advance();
        PlayerActorRotationSample sample = progress.sample(active.startYaw(), active.startPitch(), request.yaw(),
                request.pitch(), request.durationTicks(), elapsed, request.interpolation());
        apply(player, sample.yaw(), sample.pitch());
        snapshot(player, moving, Optional.of(request.commandId()));
        return sample.complete() || active.elapsedTicks() >= request.durationTicks();
    }

    private void apply(ServerPlayer player, double yaw, double pitch) {
        player.setYRot((float) yaw);
        player.setYHeadRot((float) yaw);
        player.setYBodyRot((float) yaw);
        player.setXRot((float) pitch);
    }

    private PlayerActorSnapshot snapshot(
            ServerPlayer player,
            boolean moving,
            Optional<PlayerActorCommandId> activeCommand
    ) {
        revision += 1L;
        return snapshots.snapshot(player, moving, activeCommand, revision);
    }
}
