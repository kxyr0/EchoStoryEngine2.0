package io.github.kxyr0.echotriggerseatingadapter.forge;

import io.github.kxyr0.echoseating.forge.api.lifecycle.PlayerSeatingLifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoTriggerSeatingAdapterMod.MOD_ID)
public final class EchoTriggerSeatingAdapterMod {
    public static final String MOD_ID = "echo_trigger_seating_adapter";
    private final SeatingTriggerEventProvider provider = new SeatingTriggerEventProvider();
    private MinecraftServer server;

    public EchoTriggerSeatingAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onSeatingLifecycle);
    }

    private void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        if (server == event.getServer()) {
            server = null;
        }
    }

    private void onSeatingLifecycle(PlayerSeatingLifecycleEvent event) {
        if (server != null) {
            provider.accept(event.seatEvent(), server);
        }
    }
}
