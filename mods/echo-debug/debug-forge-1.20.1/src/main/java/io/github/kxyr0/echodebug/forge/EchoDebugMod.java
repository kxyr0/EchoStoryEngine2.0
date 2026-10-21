package io.github.kxyr0.echodebug.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.forge.client.EchoDebugClient;
import io.github.kxyr0.echodebug.forge.command.EchoDebugCommands;
import io.github.kxyr0.echodebug.forge.config.EchoDebugConfig;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationService;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echodebug.forge.registry.EchoDebugItems;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandInteractionEvents;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandSessionStore;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoDebugMod.MOD_ID)
public final class EchoDebugMod {
    public static final String MOD_ID = "echo_debug";
    public static final String MOD_VERSION = "0.4.2";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAVIGATION_EVENTS =
            "io.github.kxyr0.echodebug.forge.navigation.EchoNavigationTrailEvents";
    private static final String CUTSCENE_COMMANDS =
            "io.github.kxyr0.echodebug.forge.cutscene.EchoCutsceneDebugCommands";
    private static final String STORY_COMMANDS =
            "io.github.kxyr0.echodebug.forge.command.EchoStoryDebugCommands";
    private final TriggerWandSessionStore sessions = new TriggerWandSessionStore();
    private final TriggerCreationService creationService = new TriggerCreationService();

    public EchoDebugMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EchoDebugConfig.CLIENT_SPEC);
        EchoDebugItems.register(modBus);
        EchoDebugNetwork.register(sessions, creationService);
        registerOptionalNavigationIntegration();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.register(new TriggerWandInteractionEvents(sessions));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EchoDebugClient.bootstrap(modBus));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoDebugCommands().register(event.getDispatcher());
        registerOptionalCutsceneCommands(event.getDispatcher());
        registerOptionalStoryCommands(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        sessions.remove(event.getEntity().getUUID());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            EchoDebugNetwork.sendCatalog(player);
        }
    }

    private static void registerOptionalNavigationIntegration() {
        if (!ModList.get().isLoaded("echo_navigation")) {
            LOGGER.info("{}: Echo Navigation is not loaded; NPC movement trails are disabled.",
                    DebugDiagnosticCode.DEBUG_NAVIGATION_API_UNAVAILABLE);
            return;
        }
        try {
            Class.forName(NAVIGATION_EVENTS).getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("{}: Echo Navigation trail integration failed.",
                    DebugDiagnosticCode.DEBUG_NAVIGATION_API_UNAVAILABLE, exception);
        }
    }

    private static void registerOptionalCutsceneCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ModList.get().isLoaded("echo_cutscene")) {
            return;
        }
        try {
            Class.forName(CUTSCENE_COMMANDS).getMethod("register", CommandDispatcher.class)
                    .invoke(null, dispatcher);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Debug cutscene command integration failed.", exception);
        }
    }

    private static void registerOptionalStoryCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ModList.get().isLoaded("echo_story_engine")) {
            return;
        }
        try {
            Class.forName(STORY_COMMANDS).getMethod("register", CommandDispatcher.class)
                    .invoke(null, dispatcher);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Debug story command integration failed.", exception);
        }
    }
}
