package io.github.kxyr0.echoobservation.authoring.forge.client.render;

import io.github.kxyr0.echoobservation.authoring.forge.client.ObservationAuthoringRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ObservationAuthoringHud {
    private final ObservationAuthoringRuntime runtime;

    public ObservationAuthoringHud(ObservationAuthoringRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void render(RenderGuiEvent.Post event) {
        if (!runtime.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        graphics.fill(4, 4, Math.min(570, graphics.guiWidth() - 4), 82, 0xb0101018);
        String resource = runtime.selected().map(value -> value.id().resource().toString()).orElse("<none>");
        graphics.drawString(minecraft.font, Component.literal("OBS AUTHORING | Resource: " + resource),
                9, 9, 0xffffd166, false);
        String selected = runtime.selected().map(value -> value.symbol() + " | "
                + (value.placed() ? "PLACED" : "UNPLACED")).orElse("<no observation>");
        graphics.drawString(minecraft.font, Component.literal("Selected: " + selected), 9, 22, 0xffffffff, false);
        String id = runtime.selected().map(value -> value.id() + " | rev " + value.revision())
                .orElse("<none>");
        graphics.drawString(minecraft.font, Component.literal("ID: " + id), 9, 35, 0xffdbe6f7, false);
        graphics.drawString(minecraft.font, Component.literal("Status: " + runtime.status()),
                9, 48, 0xffffd166, false);
        String hit = runtime.preview().map(value -> "hit " + value.blockX() + " " + value.blockY() + " "
                + value.blockZ() + " / " + value.face()).orElse("raycast miss");
        graphics.drawString(minecraft.font, Component.literal(hit + " | Insert: place | Tab: picker"),
                9, 61, 0xffb8d8ff, false);
        graphics.drawString(minecraft.font, Component.literal("[ ] select | Shift+[ ] resource | Delete clear"),
                9, 72, 0xff9ebddf, false);
        runtime.selectedScreen().ifPresent(point -> {
            float scaleX = graphics.guiWidth() / (float) minecraft.getWindow().getWidth();
            float scaleY = graphics.guiHeight() / (float) minecraft.getWindow().getHeight();
            int x = (int) Math.round(point.framebufferX());
            int y = (int) Math.round(point.framebufferY());
            String label = runtime.selected().map(value -> value.id() + " r" + value.revision()).orElse("");
            int width = minecraft.font.width(label) + 6;
            graphics.pose().pushPose();
            graphics.pose().scale(scaleX, scaleY, 1.0F);
            marker(graphics, x, y, 0xffffd15c);
            graphics.fill(x + 7, y - 6, x + 7 + width, y + 7, 0xc0101018);
            graphics.drawString(minecraft.font, Component.literal(label), x + 10, y - 3,
                    0xffffd166, false);
            graphics.pose().popPose();
        });
        runtime.previewScreen().ifPresent(point -> {
            float scaleX = graphics.guiWidth() / (float) minecraft.getWindow().getWidth();
            float scaleY = graphics.guiHeight() / (float) minecraft.getWindow().getHeight();
            int x = (int) Math.round(point.framebufferX());
            int y = (int) Math.round(point.framebufferY());
            graphics.pose().pushPose();
            graphics.pose().scale(scaleX, scaleY, 1.0F);
            marker(graphics, x, y, 0xffff50d2);
            graphics.pose().popPose();
        });
    }

    private static void marker(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 8, y - 2, x + 9, y + 3, 0xe0000000);
        graphics.fill(x - 2, y - 8, x + 3, y + 9, 0xe0000000);
        graphics.fill(x - 7, y - 1, x + 8, y + 2, color);
        graphics.fill(x - 1, y - 7, x + 2, y + 8, color);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xffffffff);
    }
}
