package io.github.kxyr0.echoobservation.forge;

import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceLookupEvent;
import io.github.kxyr0.echoobservation.forge.client.ObservationClientBootstrap;
import io.github.kxyr0.echoobservation.forge.command.ObservationCommands;
import io.github.kxyr0.echoobservation.forge.network.ObservationNetwork;
import io.github.kxyr0.echoobservation.forge.resource.ObservationReloadListener;
import io.github.kxyr0.echoobservation.forge.runtime.ObservationServerRuntime;
import io.github.kxyr0.echoobservation.forge.story.ObservationStoryExtensionBootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoObservationMod.MOD_ID)
public final class EchoObservationMod {
    public static final String MOD_ID = "echo_observation";

    public EchoObservationMod() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        ObservationNetwork.register();
        forgeBus.addListener(this::reloadListeners);
        forgeBus.addListener(this::serverStarted);
        forgeBus.addListener(this::serverStopped);
        forgeBus.addListener(this::playerLogin);
        forgeBus.addListener(this::serverTick);
        forgeBus.addListener(this::commands);
        forgeBus.addListener(this::serviceLookup);
        if (ModList.get().isLoaded("echo_story_engine")) {
            ObservationStoryExtensionBootstrap.register(forgeBus, ObservationServerRuntime.instance().service());
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ObservationClientBootstrap.register(
                FMLJavaModLoadingContext.get().getModEventBus()));
    }

    private void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ObservationReloadListener());
    }

    private void serverStarted(ServerStartedEvent event) {
        ObservationServerRuntime.instance().start(event.getServer());
    }

    private void serverStopped(ServerStoppedEvent event) {
        ObservationServerRuntime.instance().stop();
    }

    private void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ObservationServerRuntime.instance().sync(player);
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ObservationServerRuntime.instance().tick();
    }

    private void commands(RegisterCommandsEvent event) {
        ObservationCommands.register(event.getDispatcher());
    }

    private void serviceLookup(ObservationServiceLookupEvent event) {
        event.provide(ObservationServerRuntime.instance().service());
    }
}
