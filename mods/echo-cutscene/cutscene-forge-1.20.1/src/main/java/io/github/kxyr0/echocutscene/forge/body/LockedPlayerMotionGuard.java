package io.github.kxyr0.echocutscene.forge.body;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class LockedPlayerMotionGuard {
    private final ServerPlayerBodyLock bodyLock;

    public LockedPlayerMotionGuard(ServerPlayerBodyLock bodyLock) {
        this.bodyLock = bodyLock;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player) {
            bodyLock.enforce(player);
        }
    }

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof ServerPlayer player
                && bodyLock.locked(player)) {
            event.setCanceled(true);
            bodyLock.enforce(player);
        }
    }
}
