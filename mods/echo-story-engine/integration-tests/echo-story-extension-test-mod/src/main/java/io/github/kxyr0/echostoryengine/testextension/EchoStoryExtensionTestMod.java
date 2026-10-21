package io.github.kxyr0.echostoryengine.testextension;

import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_story_extension_test")
public final class EchoStoryExtensionTestMod {
    public EchoStoryExtensionTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerExtension);
    }

    private void registerExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new TestExtensionFactory());
    }
}
