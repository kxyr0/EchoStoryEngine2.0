package io.github.kxyr0.echodialoguenpcadapter.clientacceptance;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.forge.client.N4ClientDialogueVisualProbe;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(N4ClientVisualAcceptanceMod.MOD_ID)
public final class N4ClientVisualAcceptanceMod {
    public static final String MOD_ID = "echo_dialogue_npc_adapter_client_acceptance";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean AUTO = Boolean.getBoolean("echoDialogueNpcAdapterClientAcceptance.auto");
    private boolean started;

    public N4ClientVisualAcceptanceMod() {
        if (AUTO) {
            MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || started) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null || minecraft.getResourceManager() == null) {
            return;
        }
        started = true;
        LOGGER.info("EDNA-CLIENT-ACCEPTANCE START visual=jdh:john_father display=\"Отец\"");
        minecraft.setScreen(N4ClientDialogueVisualProbe.screen(minecraft));
    }
}
