package io.github.kxyr0.echoiteminspection.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoiteminspection.client.session.ItemInspectionSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Draws the actual baked/custom model with stack overrides, glint and a true 3D pose. */
public final class InspectionRenderer {
    private final ItemInspectionSession session;
    private final ItemStack renderedStack;
    private final Quaternionf rotation = new Quaternionf();
    private final int modelSeed;

    public InspectionRenderer(ItemInspectionSession session) {
        this.session = session;
        renderedStack = session.snapshot().copyStack();
        modelSeed = session.definition().id().hashCode();
    }

    public void render(GuiGraphics graphics, InspectionViewport viewport, InspectionLayout layout) {
        Minecraft minecraft = Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        var model = renderer.getModel(renderedStack, minecraft.level, minecraft.player, modelSeed);
        var orientation = session.transform().orientation();
        rotation.set((float) orientation.x(), (float) orientation.y(),
                (float) orientation.z(), (float) orientation.w());
        float scale = (float) layout.renderScale(viewport, session.definition().view().scale(),
                session.transform().zoom());
        graphics.flush();
        if (layout.hasDescription()) clipModel(graphics, viewport, layout);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(layout.modelX() / viewport.guiScale(), layout.modelY() / viewport.guiScale(), 800.0);
            pose.scale(scale, -scale, scale);
            pose.mulPose(rotation);
            if (model.usesBlockLight()) Lighting.setupFor3DItems();
            else Lighting.setupForFlatItems();
            renderer.render(renderedStack, ItemDisplayContext.GUI, false, pose,
                    graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
        } finally {
            pose.popPose();
            if (layout.hasDescription()) graphics.disableScissor();
            Lighting.setupFor3DItems();
        }
    }

    private void clipModel(GuiGraphics graphics, InspectionViewport viewport, InspectionLayout layout) {
        double right = layout.sideColumn() ? layout.textX() - layout.textScale() * 12 : viewport.framebufferWidth();
        double bottom = layout.sideColumn() ? viewport.framebufferHeight() * 0.77
                : layout.textY() - layout.textScale() * 8;
        graphics.enableScissor(0, 0, (int) (right / viewport.guiScale()),
                (int) (bottom / viewport.guiScale()));
    }
}
