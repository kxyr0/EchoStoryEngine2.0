package io.github.kxyr0.echodamagenumbers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echodamagenumbers.network.DamagePopupReceivedEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

public final class DamageNumberClient {
    private static final int MAXIMUM_POPUPS = 128;
    private static final List<DamagePopup> POPUPS = new ArrayList<>();
    private static boolean nextWaveRight;

    private DamageNumberClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(DamageNumberClient::received);
        MinecraftForge.EVENT_BUS.addListener(DamageNumberClient::clientTick);
        MinecraftForge.EVENT_BUS.addListener(DamageNumberClient::renderLevel);
    }

    private static void received(DamagePopupReceivedEvent event) {
        var packet = event.packet();
        nextWaveRight = !nextWaveRight;
        POPUPS.add(new DamagePopup(packet.x(), packet.y(), packet.z(), packet.amount(), nextWaveRight));
        while (POPUPS.size() > MAXIMUM_POPUPS) POPUPS.remove(0);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        POPUPS.forEach(popup -> popup.age++);
        POPUPS.removeIf(popup -> popup.age >= DamagePopup.LIFETIME_TICKS);
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || POPUPS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (DamagePopup popup : POPUPS) renderPopup(event, buffers, minecraft.font, popup);
        buffers.endBatch();
    }

    private static void renderPopup(RenderLevelStageEvent event, MultiBufferSource buffers,
                                    Font font, DamagePopup popup) {
        float progress = popup.progress(event.getPartialTick());
        float scale = scale(progress);
        float alpha = alpha(progress);
        if (scale <= 0.01F || alpha <= 0.01F) return;
        Camera camera = event.getCamera();
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.rotation());
        double wave = Math.sin(progress * Math.PI * 2.2D) * 0.22D * popup.waveDirection;
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(popup.x + right.x * wave - cameraPosition.x,
                popup.y + progress * 0.72D - cameraPosition.y,
                popup.z + right.z * wave - cameraPosition.z);
        poses.mulPose(camera.rotation());
        float worldScale = 0.026F * scale;
        poses.scale(-worldScale, -worldScale, worldScale);
        int argb = ((int) (alpha * 255.0F) << 24) | popup.color;
        font.drawInBatch(popup.text, -font.width(popup.text) * 0.5F, 0.0F, argb, true,
                poses.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poses.popPose();
    }

    private static float scale(float progress) {
        if (progress < 0.14F) return easeOutBack(progress / 0.14F);
        if (progress < 0.74F) return 1.0F;
        return 1.0F - smoothStep((progress - 0.74F) / 0.26F);
    }

    private static float alpha(float progress) {
        return progress < 0.78F ? 1.0F : 1.0F - smoothStep((progress - 0.78F) / 0.22F);
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted + 1.70158F * shifted * shifted;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
