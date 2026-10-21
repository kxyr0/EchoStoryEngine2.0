package io.github.kxyr0.echoplayerfollow.client;

import io.github.kxyr0.echoplayerfollow.client.targeting.FollowTargeting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;

final class FollowHudOverlay {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SHADOW = 0x99000000;
    private static final int CURSOR_Y_OFFSET = 50;
    private final ClientFollowState state;

    FollowHudOverlay(ClientFollowState state) {
        this.state = state;
    }

    void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        if (state.active().isPresent()) {
            return;
        }
        state.prompt(Util.getMillis()).filter(FollowTargeting::canUsePrompt)
                .ifPresent(prompt -> prompt(graphics, width, height));
    }

    private void prompt(GuiGraphics graphics, int width, int height) {
        Component keyName = FollowKeyMappings.FOLLOW.getTranslatedKeyMessage();
        Component text = Component.translatable("hud.echo_player_follow.prompt", keyName);
        int textWidth = Minecraft.getInstance().font.width(text);
        int left = (width - textWidth) / 2;
        int top = height / 2 + CURSOR_Y_OFFSET;
        graphics.drawString(Minecraft.getInstance().font, text, left, top + 1, SHADOW, false);
        graphics.drawString(Minecraft.getInstance().font, text, left, top, TEXT, false);
    }
}
