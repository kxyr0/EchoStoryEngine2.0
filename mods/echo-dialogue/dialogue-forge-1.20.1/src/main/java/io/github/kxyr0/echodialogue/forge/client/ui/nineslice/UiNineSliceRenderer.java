package io.github.kxyr0.echodialogue.forge.client.ui.nineslice;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderer;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiSliceBorders;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Compatibility adapter for the generic SLICED UI image renderer.
 */
public final class UiNineSliceRenderer {
    private final UiImageRenderer renderer = new UiImageRenderer();

    public void render(GuiGraphics graphics, UiNineSliceTexture texture, UiNineSliceBounds bounds) {
        render(graphics, texture, bounds, 1.0F);
    }

    public void render(GuiGraphics graphics, UiNineSliceTexture texture, UiNineSliceBounds bounds, float alpha) {
        renderer.render(graphics, image(texture), bounds(bounds), alpha);
    }

    static List<DrawSlice> slices(UiNineSliceTexture texture, UiNineSliceBounds bounds) {
        return UiImageRenderer.drawCommands(image(texture), bounds(bounds)).stream()
                .map(UiNineSliceRenderer::slice)
                .toList();
    }

    static float effectiveAlpha(float alpha) {
        return UiImageRenderer.effectiveAlpha(alpha);
    }

    private static UiImageDefinition image(UiNineSliceTexture texture) {
        UiNineSliceBorders border = texture.borders();
        return UiImageDefinition.sliced(texture.location(), texture.sourceWidth(), texture.sourceHeight(),
                new UiSliceBorders(border.left(), border.right(), border.top(), border.bottom()));
    }

    private static UiImageBounds bounds(UiNineSliceBounds bounds) {
        return new UiImageBounds(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static DrawSlice slice(UiImageDrawCommand command) {
        return new DrawSlice(command.x(), command.y(), command.targetWidth(), command.targetHeight(),
                command.sourceX(), command.sourceY(), command.sourceWidth(), command.sourceHeight());
    }

    record DrawSlice(
            int x,
            int y,
            int targetWidth,
            int targetHeight,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
    }
}
