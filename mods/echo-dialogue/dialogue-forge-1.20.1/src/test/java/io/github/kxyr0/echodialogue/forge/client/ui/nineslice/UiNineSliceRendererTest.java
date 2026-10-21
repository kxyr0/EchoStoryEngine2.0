package io.github.kxyr0.echodialogue.forge.client.ui.nineslice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class UiNineSliceRendererTest {
    private static final UiNineSliceTexture TEXTURE = new UiNineSliceTexture(
            ResourceLocation.fromNamespaceAndPath("echo_dialogue", "textures/gui/panel/test_dialogue_panel.png"),
            29, 28, new UiNineSliceBorders(6, 5, 6, 8));

    @Test
    void producesNineSlicesWithUnscaledCorners() {
        List<UiNineSliceRenderer.DrawSlice> slices =
                UiNineSliceRenderer.slices(TEXTURE, new UiNineSliceBounds(10, 20, 120, 64));

        assertEquals(9, slices.size());
        assertSlice(slices.get(0), new ExpectedSlice(10, 20, 6, 6, 0, 0, 6, 6));
        assertSlice(slices.get(2), new ExpectedSlice(125, 20, 5, 6, 24, 0, 5, 6));
        assertSlice(slices.get(6), new ExpectedSlice(10, 76, 6, 8, 0, 20, 6, 8));
        assertSlice(slices.get(8), new ExpectedSlice(125, 76, 5, 8, 24, 20, 5, 8));
    }

    @Test
    void stretchesEdgesOnlyOnOneAxisAndCenterOnBothAxes() {
        List<UiNineSliceRenderer.DrawSlice> slices =
                UiNineSliceRenderer.slices(TEXTURE, new UiNineSliceBounds(0, 0, 120, 64));

        assertSlice(slices.get(1), new ExpectedSlice(6, 0, 109, 6, 6, 0, 18, 6));
        assertSlice(slices.get(3), new ExpectedSlice(0, 6, 6, 50, 0, 6, 6, 14));
        assertSlice(slices.get(4), new ExpectedSlice(6, 6, 109, 50, 6, 6, 18, 14));
        assertSlice(slices.get(7), new ExpectedSlice(6, 56, 109, 8, 6, 20, 18, 8));
    }

    @Test
    void rejectsTargetsSmallerThanFixedBorders() {
        assertThrows(IllegalArgumentException.class,
                () -> UiNineSliceRenderer.slices(TEXTURE, new UiNineSliceBounds(0, 0, 10, 64)));
        assertThrows(IllegalArgumentException.class,
                () -> UiNineSliceRenderer.slices(TEXTURE, new UiNineSliceBounds(0, 0, 120, 13)));
    }

    @Test
    void rejectsTexturesWithoutCenterPixels() {
        assertThrows(IllegalArgumentException.class,
                () -> new UiNineSliceTexture(TEXTURE.location(), 11, 28, TEXTURE.borders()));
        assertThrows(IllegalArgumentException.class,
                () -> new UiNineSliceTexture(TEXTURE.location(), 29, 14, TEXTURE.borders()));
    }

    @Test
    void alphaIsClampedToRenderableBounds() {
        assertEquals(1.0F, UiNineSliceRenderer.effectiveAlpha(Float.NaN));
        assertEquals(0.0F, UiNineSliceRenderer.effectiveAlpha(-0.25F));
        assertEquals(0.55F, UiNineSliceRenderer.effectiveAlpha(0.55F));
        assertEquals(1.0F, UiNineSliceRenderer.effectiveAlpha(1.25F));
    }

    private static void assertSlice(UiNineSliceRenderer.DrawSlice slice, ExpectedSlice expected) {
        assertEquals(expected.x(), slice.x());
        assertEquals(expected.y(), slice.y());
        assertEquals(expected.targetWidth(), slice.targetWidth());
        assertEquals(expected.targetHeight(), slice.targetHeight());
        assertEquals(expected.sourceX(), slice.sourceX());
        assertEquals(expected.sourceY(), slice.sourceY());
        assertEquals(expected.sourceWidth(), slice.sourceWidth());
        assertEquals(expected.sourceHeight(), slice.sourceHeight());
    }

    private record ExpectedSlice(
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
