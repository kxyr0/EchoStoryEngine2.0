package io.github.kxyr0.echomagic.forge.time;

import io.github.kxyr0.echomagic.forge.config.MagicServerConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.entity.living.LivingEvent;

public final class MagicTimeDilation {
    private static final long DRAWING_TIMEOUT_NANOS = 12_000_000_000L;
    private static final int CREATURE_TICK_INTERVAL = 5;
    private final Map<UUID, DrawingWindow> drawing = new HashMap<>();

    public void update(ServerPlayer player, boolean active) {
        if (active) drawing.put(player.getUUID(), new DrawingWindow(player.serverLevel().dimension(),
                System.nanoTime() + DRAWING_TIMEOUT_NANOS));
        else drawing.remove(player.getUUID());
    }

    public void playerLeft(ServerPlayer player) { drawing.remove(player.getUUID()); }

    public void tick(MinecraftServer server) {
        drawing.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= System.nanoTime()
                || server.getPlayerList().getPlayer(entry.getKey()) == null);
    }

    public void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player || drawing.isEmpty()) return;
        MinecraftServer server = entity.getServer();
        if (server == null || server.isDedicatedServer()
                && !MagicServerConfig.ALLOW_GESTURE_SLOWDOWN_ON_DEDICATED_SERVER.get()) return;
        ResourceKey<Level> dimension = entity.level().dimension();
        boolean active = drawing.values().stream().anyMatch(window -> window.dimension().equals(dimension)
                && window.expiresAtNanos() > System.nanoTime());
        if (active && entity.level().getGameTime() % CREATURE_TICK_INTERVAL != 0L) {
            event.setCanceled(true);
        }
    }

    private record DrawingWindow(ResourceKey<Level> dimension, long expiresAtNanos) { }
}
