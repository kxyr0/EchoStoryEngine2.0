package io.github.kxyr0.echodialoguevoice;

import io.github.kxyr0.echodialogue.forge.api.binding.EchoDialogueLineBinderRegistrationEvent;
import io.github.kxyr0.echodialoguevoice.binding.VoiceLineArgumentBinder;
import io.github.kxyr0.echodialoguevoice.lifecycle.DialogueVoiceClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/** Owns only the semantic binder and committed client lifecycle bridge. */
@Mod(EchoDialogueVoiceAdapterMod.MOD_ID)
public final class EchoDialogueVoiceAdapterMod {
    public static final String MOD_ID = "echo_dialogue_voice_adapter";

    public EchoDialogueVoiceAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerBinder);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> DialogueVoiceClientBootstrap::bootstrap);
    }

    private void registerBinder(EchoDialogueLineBinderRegistrationEvent event) {
        event.register(new VoiceLineArgumentBinder());
    }
}
