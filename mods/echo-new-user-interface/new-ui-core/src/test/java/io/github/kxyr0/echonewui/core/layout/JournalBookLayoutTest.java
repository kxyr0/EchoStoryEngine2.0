package io.github.kxyr0.echonewui.core.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class JournalBookLayoutTest {
    private static final UiLayoutConfig CONFIG = UiLayoutConfig.defaults();
    private final JournalBookLayout layout = new JournalBookLayout();

    @Test
    void referenceResolutionMatchesAuthoredRectangle() {
        JournalBookLayoutResult result = layout.layout(viewport(1920, 1080), CONFIG);

        assertEquals(1.0D, result.scale(), 0.0D);
        assertRect(result.physicalRect(), 568, 234, 784, 612);
        assertCentered(result);
        assertFalse(result.clipped());
    }

    @Test
    void highResolutionScalesUniformly() {
        JournalBookLayoutResult result = layout.layout(viewport(3840, 2160), CONFIG);

        assertEquals(2.0D, result.scale(), 0.0D);
        assertRect(result.physicalRect(), 1136, 468, 1568, 1224);
        assertCentered(result);
    }

    @Test
    void requiredResolutionMatrixIsCenteredAndVisible() {
        int[][] sizes = {
                {2560, 1440},
                {1366, 768},
                {1280, 1024},
                {3440, 1440},
                {1024, 768},
                {1080, 1920},
                {1919, 1079}
        };

        for (int[] size : sizes) {
            JournalBookLayoutResult result = layout.layout(viewport(size[0], size[1]), CONFIG);

            assertCentered(result);
            assertFalse(result.clipped(), size[0] + "x" + size[1]);
            assertTrue(result.scale() > 0.0D, size[0] + "x" + size[1]);
            assertAspectRatio(result, size[0] + "x" + size[1]);
        }
    }

    @Test
    void oddFramebufferDimensionsRemainPhysicallyCentered() {
        JournalBookLayoutResult result = layout.layout(viewport(1919, 1079), CONFIG);

        assertTrue(result.centerErrorX() <= 0.5D);
        assertTrue(result.centerErrorY() <= 0.5D);
    }

    private static UiViewport viewport(int width, int height) {
        return new UiViewport(width, height, width, height, 1.0D, 1L);
    }

    private static void assertRect(UiRectangle rect, int x, int y, int width, int height) {
        assertEquals(new UiRectangle(x, y, width, height), rect);
    }

    private static void assertCentered(JournalBookLayoutResult result) {
        assertTrue(result.centerErrorX() <= 0.5D, "center x error=" + result.centerErrorX());
        assertTrue(result.centerErrorY() <= 0.5D, "center y error=" + result.centerErrorY());
    }

    private static void assertAspectRatio(JournalBookLayoutResult result, String label) {
        double expected = CONFIG.referenceBookWidth() / (double) CONFIG.referenceBookHeight();
        double actual = result.physicalRect().width() / (double) result.physicalRect().height();
        assertEquals(expected, actual, 0.01D, label);
    }
}
