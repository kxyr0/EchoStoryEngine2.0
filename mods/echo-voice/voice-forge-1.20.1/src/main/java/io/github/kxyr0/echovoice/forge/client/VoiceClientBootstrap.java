package io.github.kxyr0.echovoice.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.api.EchoVoiceServiceLookupEvent;
import io.github.kxyr0.echovoice.forge.command.EchoVoiceClientCommands;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;

/** Registers client-only events without loading Minecraft client classes on a dedicated server. */
public final class VoiceClientBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static VoiceClientRuntime runtime;

    private VoiceClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(VoiceClientBootstrap::setup);
        modBus.addListener(VoiceClientBootstrap::soundEngineLoaded);
        MinecraftForge.EVENT_BUS.addListener(VoiceClientBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(VoiceClientBootstrap::logout);
        MinecraftForge.EVENT_BUS.addListener(VoiceClientBootstrap::worldUnload);
        MinecraftForge.EVENT_BUS.addListener(VoiceClientBootstrap::commands);
        MinecraftForge.EVENT_BUS.addListener(VoiceClientBootstrap::serviceLookup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                runtime = VoiceClientRuntime.create(Minecraft.getInstance());
            } catch (IOException exception) {
                LOGGER.error("Echo Voice client runtime could not start.", exception);
            }
        });
    }

    private static void soundEngineLoaded(SoundEngineLoadEvent event) {
        if (runtime != null) {
            Minecraft.getInstance().execute(runtime::soundEngineReloaded);
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (runtime != null && event.phase == TickEvent.Phase.END) {
            runtime.tick(Minecraft.getInstance());
        }
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (runtime != null) {
            runtime.stop(VoiceStopReason.PLAYER_DISCONNECTED);
        }
    }

    private static void worldUnload(LevelEvent.Unload event) {
        if (runtime != null && event.getLevel().isClientSide()) {
            runtime.stop(VoiceStopReason.WORLD_UNLOADED);
        }
    }

    private static void commands(RegisterClientCommandsEvent event) {
        if (runtime != null) {
            EchoVoiceClientCommands.register(event, runtime);
        }
    }

    private static void serviceLookup(EchoVoiceServiceLookupEvent event) {
        if (runtime != null) {
            event.provide(runtime.service(), runtime.query());
            event.provideAmbient(runtime.ambient());
        }
    }
}
