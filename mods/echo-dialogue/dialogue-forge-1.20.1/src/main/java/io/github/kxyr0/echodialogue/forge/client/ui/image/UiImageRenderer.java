package io.github.kxyr0.echodialogue.forge.client.ui.image;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kxyr0.echodialogue.forge.client.ui.image.paint.UiFilledImagePainter;
import io.github.kxyr0.echodialogue.forge.client.ui.image.paint.UiSimpleImagePainter;
import io.github.kxyr0.echodialogue.forge.client.ui.image.paint.UiSlicedImagePainter;
import io.github.kxyr0.echodialogue.forge.client.ui.image.paint.UiTextureBlitter;
import io.github.kxyr0.echodialogue.forge.client.ui.image.paint.UiTiledImagePainter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Single entry point for rendering simple, sliced, tiled, and filled UI images.
 */
public final class UiImageRenderer {
    public void render(GuiGraphics graphics, UiImageDefinition image, UiImageBounds bounds) {
        render(graphics, image, bounds, 1.0F);
    }

    public void render(GuiGraphics graphics, UiImageDefinition image, UiImageBounds bounds, float alpha) {
        List<UiImageDrawCommand> commands = drawCommands(image, bounds);
        renderWithAlpha(effectiveAlpha(alpha), () -> UiTextureBlitter.render(graphics, commands));
    }

    public static List<UiImageDrawCommand> drawCommands(UiImageDefinition image, UiImageBounds bounds) {
        return switch (image.mode()) {
            case SIMPLE -> UiSimpleImagePainter.commands(image, bounds);
            case SLICED -> UiSlicedImagePainter.commands(image, bounds);
            case TILED -> UiTiledImagePainter.commands(image, bounds);
            case FILLED -> UiFilledImagePainter.commands(image, bounds);
        };
    }

    public static float effectiveAlpha(float alpha) {
        if (!Float.isFinite(alpha)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static void renderWithAlpha(float alpha, Runnable renderer) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        try {
            renderer.run();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}
