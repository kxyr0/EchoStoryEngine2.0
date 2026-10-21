package io.github.kxyr0.echosparkmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SparkMagicMovementTrail {
    private static final double MINIMUM_MOVEMENT_SQUARED = 0.0004D;
    private final Map<UUID, Vec3> previousPositions = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Vec3 current = player.position();
        Vec3 previous = previousPositions.put(player.getUUID(), current);
        if (previous == null || !SparkMagicPlayerState.enabled(player)
                || !player.isAlive() || player.isSpectator() || player.isPassenger()
                || !hasMovement(previous, current)) {
            return;
        }
        MagicBillboardParticleOptions particle = new MagicBillboardParticleOptions(
                SparkMagicParticles.SPARK_CIRCLE.get(), 1.0F, 1.0F, 1.0F,
                0.16F, 7, 0.90F, 0.0F);
        player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 0.08D,
                player.getZ(), 1, 0.16D, 0.04D, 0.16D, 0.0D);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        previousPositions.remove(event.getEntity().getUUID());
    }

    static boolean hasMovement(Vec3 previous, Vec3 current) {
        return previous.distanceToSqr(current) > MINIMUM_MOVEMENT_SQUARED;
    }
}

