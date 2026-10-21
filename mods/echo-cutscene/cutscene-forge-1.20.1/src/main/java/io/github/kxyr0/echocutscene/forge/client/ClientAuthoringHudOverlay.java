package io.github.kxyr0.echocutscene.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class ClientAuthoringHudOverlay {
    private static final int TEXT = 0xE6FFFFFF;
    private static final int REC = 0xFFFF5555;
    private final ClientAuthoringSession authoring;

    ClientAuthoringHudOverlay(ClientAuthoringSession authoring) {
        this.authoring = authoring;
    }

    @SubscribeEvent
    public void onRender(RenderGuiOverlayEvent.Post event) {
        if (!authoring.active()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, "Echo Cutscene Authoring", 8, 8, TEXT, false);
        graphics.drawString(font, "id=" + authoring.cutsceneLabel(), 8, 20, TEXT, false);
        graphics.drawString(font, "speed=" + authoring.cameraSpeed(), 8, 32, TEXT, false);
        graphics.drawString(font, "mode=" + authoring.cameraMode(), 8, 44, TEXT, false);
        graphics.drawString(font, "target=" + authoring.targetLabel(), 8, 56, TEXT, false);
        graphics.drawString(font, framingText(), 8, 68, TEXT, false);
        graphics.drawString(font, pitchText(), 8, 80, TEXT, false);
        graphics.drawString(font, "F7 set target", 8, 92, TEXT, false);
        graphics.drawString(font, recordText(), 8, 104, authoring.recording() ? REC : TEXT, false);
        graphics.drawString(font, "samples=" + authoring.sampleCount(), 8, 116, TEXT, false);
    }

    private String recordText() {
        return authoring.recording() ? "F4 stop recording" : "F4 start recording";
    }

    private String pitchText() {
        return authoring.pitchFlight() ? "F6 pitch=look" : "F6 pitch=flat";
    }

    private String framingText() {
        return "framing=" + authoring.framing().horizontal() + "," + authoring.framing().vertical();
    }
}
