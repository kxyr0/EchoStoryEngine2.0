package io.github.kxyr0.echoanimation.forge.composition;

import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.client.EchoAnimationClientEvents;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupEvent;
import io.github.kxyr0.echoanimation.forge.command.EchoAnimationCommands;
import io.github.kxyr0.echoanimation.forge.face.FaceAnimationDialogueCommandBridge;
import io.github.kxyr0.echoanimation.forge.face.ServerFaceAnimationState;
import io.github.kxyr0.echoanimation.forge.network.AnimationNetworkChannel;
import io.github.kxyr0.echoanimation.forge.playback.AnimationPlaybackHost;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceDiscovery;
import io.github.kxyr0.echoanimation.forge.resource.ForgeAnimationResourceReloadListener;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Composition root for Forge lifecycle wiring.
 */
public final class ForgeAnimationCompositionRoot {
    private final AnimationCatalogState catalogState = new AnimationCatalogState();
    private final AnimationPlaybackHost playbackHost = new AnimationPlaybackHost(catalogState);
    private final ServerFaceAnimationState faceAnimations = new ServerFaceAnimationState();
    private final AnimationResourceCandidateCompiler resourceCompiler =
            new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());
    private MinecraftServer server;

    public void register(IEventBus modBus, IEventBus forgeBus) {
        java.util.Objects.requireNonNull(modBus, "modBus");
        catalogState.bindPlaybackHost(playbackHost);
        AnimationNetworkChannel.register();
        forgeBus.addListener(this::onReloadListeners);
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onServerStopping);
        forgeBus.addListener(this::onServerTick);
        forgeBus.addListener(this::onPlayerLoggedIn);
        forgeBus.addListener(this::onServiceLookup);
        FaceAnimationDialogueCommandBridge.register(forgeBus, faceAnimations, () -> Optional.ofNullable(server));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoAnimationClientEvents.register(modBus, catalogState));
    }

    private void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        playbackHost.start(server);
    }

    private void onServerStopping(ServerStoppingEvent event) {
        playbackHost.stop();
        server = null;
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            playbackHost.tick();
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            playbackHost.sendFullSnapshots(player);
            faceAnimations.sync(player);
        }
    }

    private void onServiceLookup(AnimationServiceLookupEvent event) {
        if (event.server() == server) {
            event.provide(catalogState.service());
        }
    }

    private void onReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ForgeAnimationResourceReloadListener(
                new AnimationResourceDiscovery(),
                resourceCompiler,
                catalogState,
                () -> Optional.ofNullable(server),
                FMLPaths.GAMEDIR.get().resolve("story").resolve("content-pack")));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoAnimationCommands(catalogState).register(event.getDispatcher());
    }
}
