package io.github.kxyr0.echostoryengine.blackspace.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = EchoBlackspace.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackspaceSkyRenderer {
    private static final float CUBE_RADIUS = 128.0F;
    private static final float MIDDLE_Y = 0.58F;
    private static final int OPAQUE = 255;
    private static final int TOP_R = 0x87;
    private static final int TOP_G = 0x58;
    private static final int TOP_B = 0x95;
    private static final int MIDDLE_R = 0xB3;
    private static final int MIDDLE_G = 0x79;
    private static final int MIDDLE_B = 0xBE;
    private static final int BOTTOM_R = 0x9E;
    private static final int BOTTOM_G = 0x69;
    private static final int BOTTOM_B = 0xA9;

    private BlackspaceSkyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!BlackspaceClientState.isActive()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            BlackspaceClientState.clear();
            return;
        }
        renderCameraCenteredCube(event.getPoseStack(), event.getCamera().getPosition());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (BlackspaceClientState.isActive()) {
            event.setCanceled(true);
        }
    }

    private static void renderCameraCenteredCube(PoseStack poseStack, Vec3 camera) {
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            addCube(buffer, poseStack.last().pose(), camera);
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            poseStack.popPose();
        }
    }

    private static void addCube(BufferBuilder buffer, Matrix4f pose, Vec3 camera) {
        float minX = (float) (camera.x - CUBE_RADIUS);
        float minY = (float) (camera.y - CUBE_RADIUS);
        float minZ = (float) (camera.z - CUBE_RADIUS);
        float maxX = (float) (camera.x + CUBE_RADIUS);
        float maxY = (float) (camera.y + CUBE_RADIUS);
        float maxZ = (float) (camera.z + CUBE_RADIUS);
        float cameraY = (float) camera.y;
        addQuad(buffer, pose, cameraY, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        addQuad(buffer, pose, cameraY, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ);
        addQuad(buffer, pose, cameraY, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ);
        addQuad(buffer, pose, cameraY, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        addQuad(buffer, pose, cameraY, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        addQuad(buffer, pose, cameraY, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ);
    }

    private static void addQuad(
            BufferBuilder buffer,
            Matrix4f pose,
            float cameraY,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4
    ) {
        addVertex(buffer, pose, cameraY, x1, y1, z1);
        addVertex(buffer, pose, cameraY, x2, y2, z2);
        addVertex(buffer, pose, cameraY, x3, y3, z3);
        addVertex(buffer, pose, cameraY, x4, y4, z4);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f pose, float cameraY, float x, float y, float z) {
        float localY = y - cameraY;
        float vertical = Math.min(1.0F, Math.max(0.0F, (localY + CUBE_RADIUS) / (CUBE_RADIUS * 2.0F)));
        int red = gradient(TOP_R, MIDDLE_R, BOTTOM_R, vertical);
        int green = gradient(TOP_G, MIDDLE_G, BOTTOM_G, vertical);
        int blue = gradient(TOP_B, MIDDLE_B, BOTTOM_B, vertical);
        buffer.vertex(pose, x, y, z).color(red, green, blue, OPAQUE).endVertex();
    }

    private static int gradient(int top, int middle, int bottom, float vertical) {
        if (vertical < MIDDLE_Y) {
            return mix(bottom, middle, smoothstep(0.0F, MIDDLE_Y, vertical));
        }
        return mix(middle, top, smoothstep(MIDDLE_Y, 1.0F, vertical));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float progress = Math.min(1.0F, Math.max(0.0F, (value - edge0) / (edge1 - edge0)));
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static int mix(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }
}
