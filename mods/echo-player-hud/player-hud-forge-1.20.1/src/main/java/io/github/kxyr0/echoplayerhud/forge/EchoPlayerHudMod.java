package io.github.kxyr0.echoplayerhud.forge;

import io.github.kxyr0.echoplayerhud.forge.api.PlayerHudServiceBridge;
import io.github.kxyr0.echoplayerhud.forge.api.PlayerHudServiceLookupEvent;
import io.github.kxyr0.echoplayerhud.forge.client.PlayerHudClientBootstrap;
import io.github.kxyr0.echoplayerhud.forge.command.PlayerHudCommandRegistration;
import io.github.kxyr0.echoplayerhud.forge.config.PlayerHudConfigLoader;
import io.github.kxyr0.echoplayerhud.forge.experience.ExperienceRemovalEvents;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint for Echo Player HUD.
 */
@Mod(EchoPlayerHudMod.MOD_ID)
public final class EchoPlayerHudMod {
    public static final String MOD_ID = "echo_player_hud";
    private final ForgePlayerHudServices services = new ForgePlayerHudServices();
    private final PlayerHudConfigLoader configLoader = PlayerHudConfigLoader.create();

    public EchoPlayerHudMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        services.reloadLayout(configLoader.load(), configLoader.configPath().toString());
        PlayerHudServiceBridge.attach(services);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        MinecraftForge.EVENT_BUS.register(new ExperienceRemovalEvents());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                PlayerHudClientBootstrap.bootstrap(modBus, services, configLoader));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new PlayerHudCommandRegistration(services, configLoader).register(event.getDispatcher());
    }

    private void onServiceLookup(PlayerHudServiceLookupEvent event) {
        event.provide(services);
    }
}
