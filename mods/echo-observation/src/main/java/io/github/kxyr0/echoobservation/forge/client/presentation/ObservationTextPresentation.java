package io.github.kxyr0.echoobservation.forge.client.presentation;

import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTransientLineEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class ObservationTextPresentation {
    private String text = "";
    private long expiresAt;

    public void show(String observationId, String value, long expiresAtMillis) {
        if (ModList.get().isLoaded("echo_dialogue")) {
            long duration = Math.max(1L, Math.min(60_000L, expiresAtMillis - System.currentTimeMillis()));
            MinecraftForge.EVENT_BUS.post(new EchoDialogueTransientLineEvent(
                    observationId, Component.translatable("overlay.echo_observation.speaker").getString(),
                    value, duration));
            clear();
            return;
        }
        text = value;
        expiresAt = expiresAtMillis;
    }

    public void render(GuiGraphics graphics) {
        if (text.isBlank() || System.currentTimeMillis() >= expiresAt) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.font.width(text);
        int x = (minecraft.getWindow().getWidth() - width) / 2;
        int y = (int) Math.round(minecraft.getWindow().getHeight() * 0.72D);
        float scaleX = minecraft.getWindow().getGuiScaledWidth() / (float) minecraft.getWindow().getWidth();
        float scaleY = minecraft.getWindow().getGuiScaledHeight() / (float) minecraft.getWindow().getHeight();
        graphics.pose().pushPose();
        graphics.pose().scale(scaleX, scaleY, 1.0F);
        graphics.fill(x - 7, y - 5, x + width + 7, y + minecraft.font.lineHeight + 5, 0xb0000000);
        graphics.drawString(minecraft.font, text, x, y, 0xffffffff, true);
        graphics.pose().popPose();
    }

    public void clear() {
        text = "";
        expiresAt = 0L;
    }
}
