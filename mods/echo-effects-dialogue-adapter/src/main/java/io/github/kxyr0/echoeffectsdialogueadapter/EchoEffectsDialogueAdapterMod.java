package io.github.kxyr0.echoeffectsdialogueadapter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Bridges Echo Dialogue effect commands into Echo Effects.
 */
@Mod(EchoEffectsDialogueAdapterMod.MOD_ID)
public final class EchoEffectsDialogueAdapterMod {
    public static final String MOD_ID = "echo_effects_dialogue_adapter";

    public EchoEffectsDialogueAdapterMod() {
        MinecraftForge.EVENT_BUS.register(new EffectsDialogueWaitListener());
    }
}
