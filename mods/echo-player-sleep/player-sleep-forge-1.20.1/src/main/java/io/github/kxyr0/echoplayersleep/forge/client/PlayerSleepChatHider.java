package io.github.kxyr0.echoplayersleep.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.event.TickEvent;

public final class PlayerSleepChatHider {
    private PlayerSleepChatHider() {
    }

    public static boolean shouldHideChat() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isSleeping();
    }

    public static boolean shouldBlockScreen(Screen screen) {
        return shouldHideChat() && screen instanceof ChatScreen;
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shouldHideChat()
                && Minecraft.getInstance().screen instanceof ChatScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
