package io.github.kxyr0.echotime.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceLookupEvent;
import io.github.kxyr0.echotime.forge.command.EchoTimeCommandRegistration;
import io.github.kxyr0.echotime.forge.host.ForgeEchoTimeHost;
import io.github.kxyr0.echotime.forge.host.ForgeEchoTimeServices;
import io.github.kxyr0.echotime.forge.network.EchoTimeNetwork;
import io.github.kxyr0.echotime.script.TimeScriptIntegration;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoTimeMod.MOD_ID)
public final class EchoTimeMod {
    public static final String MOD_ID = "echo_time";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final EchoTimeNetwork network = new EchoTimeNetwork();
    private final TimeScriptIntegration scripts = new TimeScriptIntegration();
    private ForgeEchoTimeHost host;
    private ForgeEchoTimeServices services;

    public EchoTimeMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EchoTimeCommandRegistration.register(event.getDispatcher(), () -> host,
                () -> services == null ? null : services.bridge());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            host = new ForgeEchoTimeHost(event.getServer(), network);
            services = new ForgeEchoTimeServices(host);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Echo Time failed to initialize.", exception);
            host = null;
            services = null;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && host != null) host.tick();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (host != null && event.getEntity() instanceof ServerPlayer player) {
            network.send(player, host.snapshot());
        }
    }

    @SubscribeEvent
    public void onLookup(EchoTimeServiceLookupEvent event) {
        if (services != null && host != null && host.owns(event.server())) {
            event.provide(services.bridge());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (host == null) return;
        try {
            host.close();
        } catch (IOException exception) {
            LOGGER.error("Echo Time failed to close config watcher.", exception);
        } finally {
            host = null;
            services = null;
        }
    }
}
