package io.github.kxyr0.echoplayeractor.forge.placement;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public final class PlayerSpawnCommand {
    private final ServerPlayerBodyResolver resolver;
    private final Function<ServerPlayer, PlayerActorSnapshot> snapshots;

    public PlayerSpawnCommand(
            ServerPlayerBodyResolver resolver,
            Function<ServerPlayer, PlayerActorSnapshot> snapshots
    ) {
        this.resolver = resolver;
        this.snapshots = snapshots;
    }

    public PlayerActorResult spawnAt(PlayerActorSpawnRequest request) {
        return resolver.resolve(request.actorId()).map(player -> place(player, request))
                .orElseGet(() -> PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE,
                        "Player actor is offline."));
    }

    private PlayerActorResult place(ServerPlayer player, PlayerActorSpawnRequest request) {
        ServerLevel level = player.serverLevel();
        if (player.isPassenger()) {
            player.stopRiding();
        }
        Vector3 position = request.position();
        BlockPos block = BlockPos.containing(position.x(), position.y(), position.z());
        PlayerActorResult validation = validate(level, player, position, block);
        if (validation != null) {
            return validation;
        }
        level.getChunk(block);
        player.teleportTo(level, position.x(), position.y(), position.z(), Set.of(),
                (float) request.yaw(), (float) request.pitch());
        player.setYHeadRot((float) request.yaw());
        player.setYBodyRot((float) request.yaw());
        return PlayerActorResult.success(PlayerActorOutcome.SPAWNED, snapshots.apply(player),
                "Player actor placed at authored scene position.");
    }

    private PlayerActorResult validate(ServerLevel level, ServerPlayer player, Vector3 position, BlockPos block) {
        if (position.y() < level.getMinBuildHeight() || position.y() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(block)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SPAWN_POSITION_INVALID,
                    "Spawn position is outside current world bounds: " + position + ".");
        }
        AABB targetBox = player.getBoundingBox().move(position.x() - player.getX(), position.y() - player.getY(),
                position.z() - player.getZ());
        if (!level.noCollision(player, targetBox)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SPAWN_COLLISION,
                    "Spawn position collides with blocks: " + position + ".");
        }
        return null;
    }
}
