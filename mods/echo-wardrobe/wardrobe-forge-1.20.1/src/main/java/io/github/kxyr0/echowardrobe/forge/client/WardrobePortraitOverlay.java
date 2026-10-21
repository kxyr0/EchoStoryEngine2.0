package io.github.kxyr0.echowardrobe.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitOverlayContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitStrip;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws only the authored glasses pixels from 64x64 wardrobe skins over a dialogue SkinHead.
 */
final class WardrobePortraitOverlay {
    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int HEAD_GRID_SIZE = 8;
    private static final int TUNED_HEAD_RENDER_SIZE = 122;
    private static final int OFFSET_REFERENCE_HEAD_RENDER_SIZE = 32;

    private final ResourceLocation texture;
    private final List<WearablePortraitStrip> strips;

    private WardrobePortraitOverlay(ResourceLocation texture, List<WearablePortraitStrip> strips) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.strips = List.copyOf(strips);
    }

    static Optional<WardrobePortraitOverlay> from(WearablePortraitOverlayContribution contribution) {
        try {
            return Optional.of(new WardrobePortraitOverlay(location(contribution.textureResourceId()),
                    contribution.strips()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static ResourceLocation location(String raw) {
        int separator = raw.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(raw.substring(0, separator), raw.substring(separator + 1));
    }

    void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        strips.forEach(strip -> draw(graphics, strip, x, y, width, height));
    }

    private void draw(GuiGraphics graphics, WearablePortraitStrip strip, int x, int y, int width, int height) {
        if (strip.targetHeightScale() <= 0D) {
            return;
        }
        Rect target = scaledTargetRect(strip, x, y, width, height);
        Rect reference = referenceRect(strip, OFFSET_REFERENCE_HEAD_RENDER_SIZE);
        double horizontalScale = target.width() / (double) reference.width();
        double verticalScale = target.height() / (double) reference.height();
        double leftInset = strip.pixelLeftInset() * horizontalScale;
        double rightInset = strip.pixelRightInset() * horizontalScale;
        double targetWidth = Math.max(1.0D, target.width() - leftInset - rightInset);
        double targetHeight = target.height() * strip.targetHeightScale();
        graphics.pose().pushPose();
        graphics.pose().translate(target.x() + strip.pixelXOffset() * horizontalScale + leftInset,
                target.y() + strip.pixelYOffset() * verticalScale, 0D);
        graphics.pose().scale((float) (targetWidth / strip.width()), (float) (targetHeight / strip.height()), 1.0F);
        graphics.blit(texture, 0, 0, strip.width(), strip.height(), strip.sourceX(), strip.sourceY(),
                strip.width(), strip.height(), SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE);
        graphics.pose().popPose();
    }

    private static Rect scaledTargetRect(WearablePortraitStrip strip, int x, int y, int width, int height) {
        Rect tuned = referenceRect(strip, TUNED_HEAD_RENDER_SIZE);
        double scaleX = width / (double) TUNED_HEAD_RENDER_SIZE;
        double scaleY = height / (double) TUNED_HEAD_RENDER_SIZE;
        return new Rect(x + tuned.x() * scaleX, y + tuned.y() * scaleY,
                tuned.width() * scaleX, tuned.height() * scaleY);
    }

    private static Rect targetRect(WearablePortraitStrip strip, Bounds layer) {
        int left = grid(layer.x(), layer.width(), strip.targetX());
        int top = grid(layer.y(), layer.height(), strip.targetY());
        int right = grid(layer.x(), layer.width(), strip.targetX() + strip.width());
        int bottom = grid(layer.y(), layer.height(), strip.targetY() + strip.height());
        return new Rect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private static Rect referenceRect(WearablePortraitStrip strip, int size) {
        Bounds reference = strip.outerLayer() ? outerBounds(0, 0, size, size) : new Bounds(0, 0, size, size);
        return targetRect(strip, reference);
    }

    private static int grid(int origin, int size, int coordinate) {
        return origin + Math.round(size * coordinate / (float) HEAD_GRID_SIZE);
    }

    private static Bounds outerBounds(int x, int y, int width, int height) {
        int inflate = Math.max(1, Math.round(Math.min(width, height) / 16.0F));
        return new Bounds(x - inflate, y - inflate, width + inflate * 2, height + inflate * 2);
    }

    private record Bounds(int x, int y, int width, int height) {
    }

    private record Rect(double x, double y, double width, double height) {
    }
}
