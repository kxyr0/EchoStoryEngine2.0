package io.github.kxyr0.echoiteminspection.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldInspectionPromptTest {
    @Test void fadeInAndOutAreSmoothAndBounded() {
        var state = new WorldInspectionPromptState<String>();
        state.update("letter", 1000);
        assertEquals(0, state.alpha(1000));
        assertEquals(0.5, state.alpha(1070), 0.000001);
        assertEquals(1, state.alpha(1140));
        state.update(null, 1240);
        assertEquals(0.5, state.alpha(1310), 0.000001);
        assertEquals(0, state.alpha(1380));
    }

    @Test void jitterGraceIsVisualOnly() {
        var state = new WorldInspectionPromptState<String>();
        state.update("letter", 1000);
        state.update("letter", 1200);
        state.update(null, 1250);
        assertNull(state.activeTarget());
        assertEquals("letter", state.displayTarget());
        assertEquals(1, state.alpha(1250));
        state.update("letter", 1280);
        assertEquals(1, state.alpha(1280));
    }

    @Test void foregroundClearHidesImmediately() {
        var state = new WorldInspectionPromptState<String>();
        state.update("letter", 1000);
        state.clear();
        assertNull(state.activeTarget());
        assertNull(state.displayTarget());
        assertEquals(0, state.alpha(2000));
    }

    @Test void fadeReversalHasNoOpacityJump() {
        var state = new WorldInspectionPromptState<String>();
        state.update("first", 1000);
        state.update(null, 1100);
        double previous = state.alpha(1150);
        state.update("second", 1150);
        assertEquals(previous, state.alpha(1150));
        assertEquals("second", state.activeTarget());
        assertEquals(1, state.alpha(1290));
    }

    @Test void safeMarginsMatchAllRequestedResolutions() {
        for (int[] size : new int[][] {{1920, 1080}, {1280, 720}, {2560, 1440}, {3440, 1440}}) {
            var layout = WorldInspectionPromptLayout.resolve(size[0], size[1], 80);
            assertTrue(layout.x() > 0);
            assertTrue(layout.y() > 0);
            assertTrue(layout.x() + layout.width() < size[0]);
            assertEquals(Math.round(32 * layout.scale()), size[1] - layout.y() - layout.height());
        }
    }

    @Test void guiScaleCannotChangePhysicalBounds() {
        var expected = WorldInspectionPromptLayout.resolve(1920, 1080, 80);
        for (double guiScale : new double[] {1, 2, 3, 4, 5}) {
            double logicalX = expected.x() / guiScale;
            double logicalY = expected.y() / guiScale;
            assertEquals(expected.x(), logicalX * guiScale, 0.000001);
            assertEquals(expected.y(), logicalY * guiScale, 0.000001);
        }
        assertEquals(24, expected.x());
        assertEquals(1012, expected.y());
    }

    @Test void userScaleResizesPanelAndTextWithoutMovingSafeMargins() {
        var normal = WorldInspectionPromptLayout.resolve(1920, 1080, 80);
        var larger = WorldInspectionPromptLayout.resolve(1920, 1080, 80, 1.5, 0, 36);
        assertEquals(normal.width() * 1.5, larger.width());
        assertEquals(54, larger.height());
        assertEquals(1.5, larger.scale());
        assertEquals(24, larger.x());
        assertEquals(32, 1080 - larger.y() - larger.height());
    }

    @Test void independentWidthAndHeightPreserveFontScaleAndContent() {
        var custom = WorldInspectionPromptLayout.resolve(1920, 1080, 80, 1, 360, 60);
        assertEquals(360, custom.width());
        assertEquals(60, custom.height());
        assertEquals(1, custom.scale());
        var smallWidth = WorldInspectionPromptLayout.resolve(1920, 1080, 80, 1, 10, 24);
        assertEquals(184, smallWidth.width());
    }

    @Test void oversizedPromptsFitSmallWindowsWithoutClippingText() {
        for (int textWidth : new int[] {80, 2000}) {
            var layout = WorldInspectionPromptLayout.resolve(1280, 720, textWidth, 3, 1200, 120);
            assertTrue(layout.x() >= 0);
            assertTrue(layout.y() >= 0);
            assertTrue(layout.x() + layout.width() <= 1280);
            assertTrue(layout.y() + layout.height() <= 720);
            assertTrue(layout.width() + 0.5 >= (textWidth * 2 + 24) * layout.scale());
        }
    }

    @Test void invalidPresentationSettingsAreRejected() {
        for (double scale : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> WorldInspectionPromptLayout.resolve(1920, 1080, 80, scale, 0, 36));
        }
        assertThrows(IllegalArgumentException.class,
                () -> WorldInspectionPromptLayout.resolve(1920, 1080, 80, 1, -1, 36));
        assertThrows(IllegalArgumentException.class,
                () -> WorldInspectionPromptLayout.resolve(1920, 1080, 80, 1, 0, 23));
    }

    @Test void invalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorldInspectionPromptLayout.resolve(0, 1080, 10));
        assertThrows(IllegalArgumentException.class, () -> WorldInspectionPromptLayout.resolve(1920, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> WorldInspectionPromptLayout.resolve(1920, 1080, -1));
    }
}
