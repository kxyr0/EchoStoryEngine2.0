package io.github.kxyr0.echovisibility.forge;

import io.github.kxyr0.echovisibility.forge.client.VisibilityClientBootstrap;
import io.github.kxyr0.echovisibility.forge.command.VisibilityServerCommands;
import io.github.kxyr0.echovisibility.forge.network.EchoVisibilityNetwork;
import io.github.kxyr0.echovisibility.forge.network.VisibilityReportNetworkEvent;
import io.github.kxyr0.echovisibility.forge.runtime.VisibilityServerHost;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(EchoVisibilityMod.MOD_ID)
public final class EchoVisibilityMod {
    public static final String MOD_ID = "echo_visibility";
    private VisibilityServerHost host;

    public EchoVisibilityMod() {
        EchoVisibilityNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::logout);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::report);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::lookup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VisibilityClientBootstrap::register);
    }

    private void started(ServerStartedEvent event) {
        host = new VisibilityServerHost(event.getServer(), FMLPaths.GAMEDIR.get());
        host.reload();
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            host.connect(player);
        }
    }

    private void stopped(ServerStoppedEvent event) {
        host = null;
    }

    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (host != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            host.connect(player);
        }
    }

    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (host != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            host.disconnect(player);
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (host != null && event.phase == TickEvent.Phase.END) {
            host.tick();
        }
    }

    private void report(VisibilityReportNetworkEvent event) {
        if (host != null) {
            host.accept(event);
        }
    }

    private void commands(RegisterCommandsEvent event) {
        if (host != null) {
            VisibilityServerCommands.register(event.getDispatcher(), host);
        }
    }

    private void lookup(io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent event) {
        if (host != null) {
            host.lookup(event);
        }
    }
}
