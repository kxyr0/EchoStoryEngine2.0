package io.github.kxyr0.echodialoguecutscene.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialoguecutscene.forge.command.EchoEscapeCommands;
import io.github.kxyr0.echodialoguecutscene.forge.screen.ScreenCommandScheduler;
import io.github.kxyr0.echodialoguecutscene.forge.screen.ScreenStoryExtensionFactory;
import io.github.kxyr0.echodialoguecutscene.forge.wait.DialogueCutsceneServices;
import io.github.kxyr0.echodialoguecutscene.forge.wait.DialogueCutsceneWaitBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoDialogueCutsceneAdapterMod.MOD_ID)
public final class EchoDialogueCutsceneAdapterMod {
    public static final String MOD_ID = "echo_dialogue_cutscene_adapter";
    public static final String MOD_VERSION = "0.1.1";
    public static final Logger LOGGER = LogUtils.getLogger();
    private final DialogueCutsceneServices services;
    private final ScreenCommandScheduler screenScheduler;

    public EchoDialogueCutsceneAdapterMod() {
        services = new DialogueCutsceneServices();
        screenScheduler = new ScreenCommandScheduler(services, LOGGER);
        MinecraftForge.EVENT_BUS.register(new DialogueCutsceneWaitBridge(services, LOGGER));
        MinecraftForge.EVENT_BUS.register(screenScheduler);
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new ScreenStoryExtensionFactory(screenScheduler));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        new EchoEscapeCommands(services).register(event.getDispatcher());
    }
}
