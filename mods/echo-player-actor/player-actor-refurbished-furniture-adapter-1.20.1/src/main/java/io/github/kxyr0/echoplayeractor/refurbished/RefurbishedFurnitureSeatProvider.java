package io.github.kxyr0.echoplayeractor.refurbished;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorPose;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSeatSnapshot;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorTransform;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProvider;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

final class RefurbishedFurnitureSeatProvider implements PlayerActorSeatProvider {
    private static final String PROVIDER_ID = "echo_player_actor:refurbished_furniture";
    private static final double CHAIR_SEAT_HEIGHT = 0.375D;

    private final RefurbishedFurnitureSeatReflection seats;

    RefurbishedFurnitureSeatProvider(RefurbishedFurnitureSeatReflection seats) {
        this.seats = seats;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supports(ServerPlayer player) {
        return true;
    }

    @Override
    public PlayerActorSeatSnapshot snapshot(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !seats.isSeat(vehicle)) {
            return PlayerActorSeatSnapshot.standing();
        }
        String seatId = vehicle.blockPosition().getX() + "," + vehicle.blockPosition().getY()
                + "," + vehicle.blockPosition().getZ();
        return new PlayerActorSeatSnapshot(true, Optional.of(PROVIDER_ID), Optional.of(seatId),
                Optional.ofNullable(vehicle.getEncodeId()));
    }

    @Override
    public PlayerActorResult sitAt(ServerPlayer player, BlockPosition position, int timeoutTicks) {
        BlockPos block = new BlockPos(position.x(), position.y(), position.z());
        if (snapshot(player).seated()) {
            return alreadySeated(player, block);
        }
        BlockState state = player.serverLevel().getBlockState(block);
        if (!isRefurbishedChair(state)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_NOT_FOUND,
                    "Target block is not a Refurbished Furniture chair.");
        }
        if (!seats.availableAt(player.serverLevel(), block)) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_OCCUPIED,
                    "Refurbished Furniture seat entity already exists at target chair.");
        }
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        boolean seated = seats.sit(player, block, CHAIR_SEAT_HEIGHT, seatDirection(state).orElse(null));
        if (!seated || !snapshot(player).seated()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SIT_REJECTED,
                    "Refurbished Furniture rejected chair seating.");
        }
        applyRotation(player, yaw, pitch);
        return PlayerActorResult.success(PlayerActorOutcome.SEATED, snapshotResult(player),
                "Player seated on Refurbished Furniture chair.");
    }

    @Override
    public PlayerActorResult standUp(ServerPlayer player) {
        if (snapshot(player).seated()) {
            player.stopRiding();
        }
        return PlayerActorResult.success(PlayerActorOutcome.STOOD_UP, snapshotResult(player),
                "Player detached from Refurbished Furniture seat.");
    }

    private PlayerActorResult alreadySeated(ServerPlayer player, BlockPos block) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null && seats.isSeat(vehicle) && vehicle.blockPosition().equals(block)) {
            return PlayerActorResult.success(PlayerActorOutcome.SEATED, snapshotResult(player),
                    "Player is already seated on target chair.");
        }
        return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_ALREADY_SEATED_ELSEWHERE,
                "Player is already seated on a different seat.");
    }

    private boolean isRefurbishedChair(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && "refurbished_furniture".equals(id.getNamespace())
                && id.getPath().endsWith("_chair");
    }

    private Optional<Direction> seatDirection(BlockState state) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            if (entry.getValue() instanceof Direction direction && isFacingProperty(entry.getKey())) {
                return Optional.of(direction.getOpposite());
            }
        }
        return Optional.empty();
    }

    private boolean isFacingProperty(Property<?> property) {
        return "direction".equals(property.getName()) || "facing".equals(property.getName());
    }

    private void applyRotation(ServerPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setXRot(pitch);
    }

    private PlayerActorSnapshot snapshotResult(ServerPlayer player) {
        PlayerActorTransform transform = new PlayerActorTransform(
                player.serverLevel().dimension().location().toString(),
                new Vector3(player.getX(), player.getY(), player.getZ()), player.getYRot(),
                player.getYHeadRot(), player.getXRot());
        return new PlayerActorSnapshot(PlayerActorId.of(player.getUUID()), transform, pose(player),
                snapshot(player), player.isUsingItem(), false, Optional.empty(), 0L);
    }

    private PlayerActorPose pose(ServerPlayer player) {
        return player.isPassenger() ? PlayerActorPose.SITTING : PlayerActorPose.STANDING;
    }
}
