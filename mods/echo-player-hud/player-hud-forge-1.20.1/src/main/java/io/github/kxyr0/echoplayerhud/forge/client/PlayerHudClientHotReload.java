package io.github.kxyr0.echoplayerhud.forge.client;

import io.github.kxyr0.echoplayerhud.forge.client.render.PlayerHudRenderer;
import io.github.kxyr0.echoplayerhud.forge.config.PlayerHudConfigLoader;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

final class PlayerHudClientHotReload {
    private static final int POLL_INTERVAL_TICKS = 20;

    private final ForgePlayerHudServices services;
    private final PlayerHudConfigLoader loader;
    private final PlayerHudRenderer renderer;
    private int ticks;
    private long lastStamp;

    private PlayerHudClientHotReload(ForgePlayerHudServices services, PlayerHudConfigLoader loader,
                                     PlayerHudRenderer renderer) {
        this.services = services;
        this.loader = loader;
        this.renderer = renderer;
        this.lastStamp = loader.currentStamp();
    }

    static void register(ForgePlayerHudServices services, PlayerHudConfigLoader loader,
                         PlayerHudRenderer renderer) {
        MinecraftForge.EVENT_BUS.addListener(new PlayerHudClientHotReload(services, loader, renderer)::onClientTick);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null) {
            return;
        }
        ticks++;
        if (ticks < POLL_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        long currentStamp = loader.currentStamp();
        if (currentStamp == lastStamp) {
            return;
        }
        lastStamp = currentStamp;
        services.reloadLayout(loader.load(), loader.configPath().toString());
        renderer.clearIconCache();
    }
}
