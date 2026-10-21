package io.github.kxyr0.echoplayerhud.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.forge.api.HudMetricProviderRegistrationEvent;
import io.github.kxyr0.echoplayerhud.forge.client.render.PlayerHudRenderer;
import io.github.kxyr0.echoplayerhud.forge.config.PlayerHudConfigLoader;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * Client-only HUD bootstrap.
 */
public final class PlayerHudClientBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PlayerHudRenderer RENDERER = new PlayerHudRenderer();

    private PlayerHudClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus, ForgePlayerHudServices services,
                                 PlayerHudConfigLoader configLoader) {
        services.register(new LocalHealthMetricProvider());
        modBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAboveAll("echo_player_hud",
                RENDERER::render));
        modBus.addListener((FMLClientSetupEvent event) -> {
            LOGGER.info("Echo Player HUD posts metric provider registration event providersBefore={}",
                    services.providers().size());
            MinecraftForge.EVENT_BUS.post(new HudMetricProviderRegistrationEvent(services));
            LOGGER.info("Echo Player HUD metric provider registration event complete providersAfter={}",
                    services.providers().size());
        });
        MinecraftForge.EVENT_BUS.addListener(VanillaVitalsHudSuppressor::onPreRender);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                new JadeOverlaySuppressor()::onRenderGuiPost);
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                PlayerHudClientCommands.register(event, RENDERER, services, configLoader));
        PlayerHudClientHotReload.register(services, configLoader, RENDERER);
        RENDERER.useServices(services);
        RENDERER.useConfigPath(configLoader.configPath());
    }
}
