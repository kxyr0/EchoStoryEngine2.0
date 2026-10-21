package io.github.kxyr0.echotimehud.forge.client;

import io.github.kxyr0.echotimehud.forge.config.HudConfigLoader;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class TimeHudClientBootstrap {
    private TimeHudClientBootstrap() { }

    public static void bootstrap(IEventBus modBus) {
        HudConfigLoader configs = HudConfigLoader.gameDirectory();
        TimeHudSnapshotStore snapshots = new TimeHudSnapshotStore();
        TimeHudRenderer renderer = new TimeHudRenderer(snapshots, configs);
        configs.start();
        modBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(), "echo_time_hud", renderer::render));
        MinecraftForge.EVENT_BUS.addListener(snapshots::accept);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> snapshots.clear());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) configs.clientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                TimeHudClientCommands.register(event, renderer, snapshots, configs));
    }

}
