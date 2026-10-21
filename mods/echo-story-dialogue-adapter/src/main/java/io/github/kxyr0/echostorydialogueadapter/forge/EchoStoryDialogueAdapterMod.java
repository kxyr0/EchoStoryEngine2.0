package io.github.kxyr0.echostorydialogueadapter.forge;

import io.github.kxyr0.echostorydialogueadapter.bridge.DialogueBridgeExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Thin Forge entrypoint that registers the Story extension factory.
 */
@Mod(EchoStoryDialogueAdapterMod.MOD_ID)
public final class EchoStoryDialogueAdapterMod {
    public static final String MOD_ID = "echo_story_dialogue_adapter";

    public EchoStoryDialogueAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new DialogueBridgeExtensionFactory());
    }
}
