package io.github.kxyr0.echodialogue.forge.client.ui.debug;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only key binding for opening the 9-slice dev screen.
 */
public final class UiNineSliceDebugInput {
    private static final String CATEGORY = "key.categories.echo_dialogue";
    private static final KeyMapping OPEN_SCREEN = new KeyMapping("key.echo_dialogue.nine_slice_debug",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY);

    private UiNineSliceDebugInput() {
    }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SCREEN);
        }
    }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_SCREEN.consumeClick()) {
                if (minecraft.screen == null && minecraft.player != null) {
                    minecraft.setScreen(new UiNineSliceDebugScreen());
                }
            }
        }
    }
}
