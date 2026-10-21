package io.github.kxyr0.echocutscene.forge.body;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public record PlayerBodySnapshot(
        UUID playerId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        Pose pose,
        Optional<UUID> vehicleId,
        int selectedSlot,
        ItemStack mainHand,
        ItemStack offHand,
        GameType gameType
) {
    public static PlayerBodySnapshot capture(ServerPlayer player) {
        Optional<UUID> vehicle = Optional.ofNullable(player.getVehicle()).map(entity -> entity.getUUID());
        return new PlayerBodySnapshot(player.getUUID(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getPose(), vehicle, player.getInventory().selected,
                player.getMainHandItem().copy(), player.getOffhandItem().copy(),
                player.gameMode.getGameModeForPlayer());
    }
}
