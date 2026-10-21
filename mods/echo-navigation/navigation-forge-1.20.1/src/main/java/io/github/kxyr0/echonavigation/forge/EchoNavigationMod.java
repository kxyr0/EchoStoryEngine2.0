package io.github.kxyr0.echonavigation.forge;

import io.github.kxyr0.echonavigation.forge.command.EchoNavigationCommands;
import io.github.kxyr0.echonavigation.forge.runtime.ForgeNavigationHost;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(EchoNavigationMod.MOD_ID)
public final class EchoNavigationMod {
    public static final String MOD_ID = "echo_navigation";
    public static final String MOD_VERSION = "0.4.0";

    private final NavigationStructuredLogger logger = new NavigationStructuredLogger();
    private ForgeNavigationHost host;

    public EchoNavigationMod() {
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::serviceLookup);
    }

    private void serverStarted(ServerStartedEvent event) {
        host = new ForgeNavigationHost(FMLPaths.GAMEDIR.get(), logger);
        host.start(event.getServer());
    }

    private void serverStopping(ServerStoppingEvent event) {
        if (host != null) {
            host.stop();
            host = null;
        }
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (host != null && event.phase == TickEvent.Phase.END) {
            host.tick(event.getServer().overworld().getGameTime());
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        new EchoNavigationCommands(() -> host).register(event.getDispatcher());
    }

    private void serviceLookup(io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceLookupEvent event) {
        if (host != null && host.server() == event.server()) {
            event.provide(host.service(), host.service(), host.surfaceRoutes());
        }
    }
}
