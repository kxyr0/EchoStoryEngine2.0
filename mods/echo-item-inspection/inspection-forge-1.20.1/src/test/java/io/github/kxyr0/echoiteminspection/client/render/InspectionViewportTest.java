package io.github.kxyr0.echoiteminspection.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InspectionViewportTest {
    @Test void centerAndModelSizeAreIndependentOfGuiScaleIncludingRoundedDimensions() {
        for (int[] resolution : new int[][] {{1280,720}, {1366,768}, {1920,1080},
                {2560,1440}, {3440,1440}}) {
            int width = resolution[0];
            int height = resolution[1];
            for (double scale : new double[] {1, 2, 3, 4}) {
                var viewport = new InspectionViewport(width, height, scale,
                        (int) Math.ceil(width / scale), (int) Math.ceil(height / scale));
                assertEquals(width / 2.0, viewport.centerX() * scale, 1e-9);
                assertEquals(height * 0.44, viewport.centerY() * scale, 1e-9);
                assertEquals(Math.min(width, height) * 0.40,
                        viewport.modelScale() * scale, 1e-9);
            }
        }
    }

    @Test void invalidFramebufferCannotEnterRendering() {
        assertThrows(IllegalArgumentException.class,
                () -> new InspectionViewport(0, 1080, 1, 1920, 1080));
        assertThrows(IllegalArgumentException.class,
                () -> new InspectionViewport(1920, 1080, Double.NaN, 1920, 1080));
    }

    @Test void extremeAuthoredScaleCannotOverflowTheRenderTransform() {
        var viewport = new InspectionViewport(1920, 1080, 2, 960, 540);
        double scale = viewport.modelScale(Double.MAX_VALUE, Double.MAX_VALUE);
        assertTrue(Double.isFinite(scale));
        assertEquals(2160, scale);
    }
}
