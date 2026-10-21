package io.github.kxyr0.echoplayeractor.forge.body;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorPose;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSeatSnapshot;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorTransform;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;

public final class PlayerActorSnapshots {
    public PlayerActorSnapshot snapshot(
            ServerPlayer player,
            boolean moving,
            Optional<PlayerActorCommandId> activeCommand,
            long revision
    ) {
        PlayerActorTransform transform = new PlayerActorTransform(dimensionId(player),
                new Vector3(player.getX(), player.getY(), player.getZ()), player.getYRot(), player.getYHeadRot(),
                player.getXRot());
        return new PlayerActorSnapshot(PlayerActorId.of(player.getUUID()), transform, pose(player),
                seat(player), player.isUsingItem(), moving, activeCommand, revision);
    }

    public String dimensionId(ServerPlayer player) {
        return player.serverLevel().dimension().location().toString();
    }

    private PlayerActorSeatSnapshot seat(ServerPlayer player) {
        if (!player.isPassenger()) {
            return PlayerActorSeatSnapshot.standing();
        }
        String vehicle = player.getVehicle() == null ? "unknown" : player.getVehicle().getEncodeId();
        return new PlayerActorSeatSnapshot(true, Optional.of("minecraft:vehicle"), Optional.empty(),
                Optional.ofNullable(vehicle));
    }

    private PlayerActorPose pose(ServerPlayer player) {
        Pose pose = player.getPose();
        if (player.isPassenger()) {
            return PlayerActorPose.SITTING;
        }
        return switch (pose) {
            case CROUCHING -> PlayerActorPose.CROUCHING;
            case FALL_FLYING -> PlayerActorPose.FALL_FLYING;
            case SWIMMING -> PlayerActorPose.SWIMMING;
            case SLEEPING -> PlayerActorPose.SLEEPING;
            default -> PlayerActorPose.STANDING;
        };
    }
}
