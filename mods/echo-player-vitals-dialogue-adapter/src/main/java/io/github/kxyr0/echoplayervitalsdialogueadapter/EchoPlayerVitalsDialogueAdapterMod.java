package io.github.kxyr0.echoplayervitalsdialogueadapter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Bridges Echo Dialogue player-scoped fatigue commands into Echo Player Vitals.
 */
@Mod(EchoPlayerVitalsDialogueAdapterMod.MOD_ID)
public final class EchoPlayerVitalsDialogueAdapterMod {
    public static final String MOD_ID = "echo_player_vitals_dialogue_adapter";

    public EchoPlayerVitalsDialogueAdapterMod() {
        MinecraftForge.EVENT_BUS.register(new PlayerVitalsDialogueWaitListener());
    }
}
