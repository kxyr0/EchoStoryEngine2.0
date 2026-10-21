package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoTriggerPlayerSleepAdapterMod.MOD_ID)
public final class EchoTriggerPlayerSleepAdapterMod {
    public static final String MOD_ID = "echo_trigger_player_sleep_adapter";
    private final SleepTriggerEventProvider provider = new SleepTriggerEventProvider();
    private MinecraftServer server;

    public EchoTriggerPlayerSleepAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onSleepAttempt);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onSleepLifecycle);
    }

    private void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        if (server == event.getServer()) {
            server = null;
        }
    }

    private void onSleepAttempt(PlayerSleepInBedEvent event) {
        if (server != null) {
            provider.prepare(event, server);
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && server == event.getServer()) {
            provider.tick(event.getServer());
        }
    }

    private void onSleepLifecycle(PlayerSleepLifecycleEvent event) {
        if (server != null) {
            provider.accept(event, server);
        }
    }
}
