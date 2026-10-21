package io.github.kxyr0.echodialoguenpcadapter;

import io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerPresentationRegistrationEvent;
import io.github.kxyr0.echodialoguenpcadapter.presentation.NpcSpeakerPresentationProviderFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoDialogueNpcAdapterMod.MOD_ID)
public final class EchoDialogueNpcAdapterMod {
    public static final String MOD_ID = "echo_dialogue_npc_adapter";
    public static final String VERSION = "0.1.0-SNAPSHOT";

    public EchoDialogueNpcAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerSpeakerPresentationProvider);
    }

    private void registerSpeakerPresentationProvider(EchoDialogueSpeakerPresentationRegistrationEvent event) {
        event.register(new NpcSpeakerPresentationProviderFactory(VERSION));
    }
}
