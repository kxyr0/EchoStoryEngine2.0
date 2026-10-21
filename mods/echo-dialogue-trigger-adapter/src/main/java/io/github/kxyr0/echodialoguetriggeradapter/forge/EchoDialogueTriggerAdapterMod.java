package io.github.kxyr0.echodialoguetriggeradapter.forge;

import io.github.kxyr0.echodialoguetriggeradapter.callback.DialogueTriggerCallbackListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_dialogue_trigger_adapter")
public final class EchoDialogueTriggerAdapterMod {
    public EchoDialogueTriggerAdapterMod() {
        MinecraftForge.EVENT_BUS.register(new DialogueTriggerCallbackListener());
    }
}
