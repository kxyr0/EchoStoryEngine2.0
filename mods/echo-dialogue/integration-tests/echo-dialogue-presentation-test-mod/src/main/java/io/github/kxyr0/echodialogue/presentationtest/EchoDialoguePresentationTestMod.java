package io.github.kxyr0.echodialogue.presentationtest;

import io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerPresentationRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_dialogue_presentation_test")
public final class EchoDialoguePresentationTestMod {
    public EchoDialoguePresentationTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerProviders);
    }

    private void registerProviders(EchoDialogueSpeakerPresentationRegistrationEvent event) {
        event.register(new EchoDialoguePresentationTestProvider("echo_dialogue_test:main_provider", false));
        event.register(new EchoDialoguePresentationTestProvider("echo_dialogue_test:ambiguity_provider", true));
    }
}
