package io.github.kxyr0.echocutscene.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneAuthoringServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.authoring.ServerCutsceneAuthoringHost;
import io.github.kxyr0.echocutscene.forge.body.BodyLockStore;
import io.github.kxyr0.echocutscene.forge.body.LockedPlayerMotionGuard;
import io.github.kxyr0.echocutscene.forge.body.ServerPlayerBodyLock;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import io.github.kxyr0.echocutscene.forge.command.CutsceneCommands;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import io.github.kxyr0.echocutscene.forge.playback.CutsceneResourceLoader;
import io.github.kxyr0.echocutscene.forge.playback.FileSystemTrackWatcher;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutscenePlaybackHost;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutsceneResourceCatalog;
import io.github.kxyr0.echocutscene.forge.service.CutsceneDiagnosticsLedger;
import io.github.kxyr0.echocutscene.forge.service.ForgeCutsceneAuthoringService;
import io.github.kxyr0.echocutscene.forge.service.ForgeCutsceneService;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoCutsceneMod.MOD_ID)
public final class EchoCutsceneMod {
    public static final String MOD_ID = "echo_cutscene";
    public static final String MOD_VERSION = "0.2.2";
    public static final Logger LOGGER = LogUtils.getLogger();
    private final BodyLockStore bodyLocks = new BodyLockStore();
    private final ServerCutsceneResourceCatalog catalog = new ServerCutsceneResourceCatalog();
    private final CutsceneResourceLoader loader = new CutsceneResourceLoader();
    private final FileSystemTrackWatcher watcher = new FileSystemTrackWatcher();
    private final CutsceneDiagnosticsLedger diagnostics = new CutsceneDiagnosticsLedger();
    private final ServerCutscenePlaybackHost host = new ServerCutscenePlaybackHost(catalog, bodyLocks, diagnostics);
    private final ServerCutsceneAuthoringHost authoring = new ServerCutsceneAuthoringHost(bodyLocks, catalog,
            loader, watcher);
    private final ForgeCutsceneService service = new ForgeCutsceneService(host);
    private final ForgeCutsceneAuthoringService authoringService = new ForgeCutsceneAuthoringService(authoring);

    public EchoCutsceneMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EchoCutsceneNetwork.register(host, authoring);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onLookup);
        MinecraftForge.EVENT_BUS.addListener(this::onAuthoringLookup);
        MinecraftForge.EVENT_BUS.register(new LockedPlayerMotionGuard(new ServerPlayerBodyLock(bodyLocks)));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EchoCutsceneClient.bootstrap(modBus));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new CutsceneCommands(host, catalog, loader, watcher, authoring).register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        host.clearRuntime();
        loadFileSystemTracks(event.getServer());
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            host.tick(event.getServer());
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        host.suspendAll(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            host.reattach(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            host.suspend(player);
            authoring.close(player);
        }
    }

    private void onLookup(CutsceneServiceLookupEvent event) {
        event.provide(service);
    }

    private void onAuthoringLookup(CutsceneAuthoringServiceLookupEvent event) {
        event.provide(authoringService);
    }

    private void loadFileSystemTracks(MinecraftServer server) {
        if (!watcher.exists(server)) {
            return;
        }
        try {
            CutsceneResourceLoader.LoadedCutscenes loaded = loader.load(watcher.root(server));
            catalog.replaceAll(loaded.definitions(), loaded.tracks());
            LOGGER.info("Echo Cutscene loaded {} filesystem camera tracks.", loaded.tracks().size());
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Echo Cutscene failed to load filesystem camera tracks.", exception);
        }
    }
}
