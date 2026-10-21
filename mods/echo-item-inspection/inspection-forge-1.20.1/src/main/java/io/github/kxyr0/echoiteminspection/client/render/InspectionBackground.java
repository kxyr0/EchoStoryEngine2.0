package io.github.kxyr0.echoiteminspection.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import java.io.IOException;

/** Screen-local adaptation of Echo Blackspace's final gradient; never switches the world shaderpack. */
public final class InspectionBackground {
    private static ShaderInstance shader;

    private InspectionBackground() { }

    public static void registerShaders(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation("echo_item_inspection", "blackspace_background"),
                    DefaultVertexFormat.POSITION), loaded -> shader = loaded);
        } catch (IOException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Inspection background shader unavailable; using gradient", exception);
        }
    }

    public static boolean shaderReady() { return shader != null; }

    public static void render(GuiGraphics graphics, int width, int height) {
        graphics.flush();
        if (shader == null) {
            graphics.fillGradient(0, 0, width, height, 0xFF27192E, 0xFF37243D);
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        try {
            RenderSystem.setShader(() -> shader);
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            var pose = graphics.pose().last().pose();
            buffer.vertex(pose, 0, height, 0).endVertex();
            buffer.vertex(pose, width, height, 0).endVertex();
            buffer.vertex(pose, width, 0, 0).endVertex();
            buffer.vertex(pose, 0, 0, 0).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }
}
