package io.github.kxyr0.echoplayersleep.forge;

import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceLookupEvent;
import io.github.kxyr0.echoplayersleep.forge.bed.VanillaBedSleepProvider;
import io.github.kxyr0.echoplayersleep.forge.client.PlayerSleepClientBootstrap;
import io.github.kxyr0.echoplayersleep.forge.command.PlayerSleepDiagnosticCommandRegistration;
import io.github.kxyr0.echoplayersleep.forge.command.PlayerSleepCommandRegistration;
import io.github.kxyr0.echoplayersleep.forge.interception.VanillaSleepInterception;
import io.github.kxyr0.echoplayersleep.forge.network.PlayerSleepNetwork;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepServices;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoPlayerSleepMod.MOD_ID)
public final class EchoPlayerSleepMod {
    public static final String MOD_ID = "echo_player_sleep";
    private final ForgePlayerSleepServices services = new ForgePlayerSleepServices();
    private final VanillaSleepInterception interception = new VanillaSleepInterception(services);

    public EchoPlayerSleepMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PlayerSleepNetwork.register();
        services.bedProviders().register(new VanillaBedSleepProvider());
        MinecraftForge.EVENT_BUS.register(interception);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> PlayerSleepClientBootstrap.bootstrap(modBus));
    }

    private void onServerStarted(ServerStartedEvent event) {
        services.useServer(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PlayerSleepCommandRegistration.register(event.getDispatcher(), services);
        PlayerSleepDiagnosticCommandRegistration.register(event.getDispatcher(), services);
    }

    private void onServiceLookup(PlayerSleepServiceLookupEvent event) {
        services.useServer(event.server());
        event.provide(services.bridge());
    }
}
