package io.github.kxyr0.echoplayeractor.forge;

import io.github.kxyr0.echoplayeractor.forge.command.PlayerActorCommands;
import io.github.kxyr0.echoplayeractor.forge.runtime.ServerPlayerActorHost;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoPlayerActorMod.MOD_ID)
public final class EchoPlayerActorMod {
    public static final String MOD_ID = "echo_player_actor";
    public static final String MOD_VERSION = "0.2.0";

    private final ServerPlayerActorHost host = new ServerPlayerActorHost();

    public EchoPlayerActorMod() {
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::serviceLookup);
    }

    private void serverStarted(ServerStartedEvent event) {
        host.start(event.getServer());
    }

    private void serverStopping(ServerStoppingEvent event) {
        host.stop();
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            host.tick(event.getServer().overworld().getGameTime());
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        new PlayerActorCommands(() -> host).register(event.getDispatcher());
    }

    private void serviceLookup(
            io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorServiceLookupEvent event
    ) {
        if (host.started(event.server())) {
            event.provide(host, host, host);
        }
    }
}
