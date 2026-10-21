package io.github.kxyr0.echoiteminspection.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InspectionLayoutTest {
    @Test void sameFramebufferKeepsTextAndModelPhysicalLayoutAcrossEveryGuiScale() {
        for (int[] size : new int[][] {{1280,720}, {1366,768}, {1920,1080},
                {2560,1440}, {3440,1440}, {854,480}, {900,1200}}) {
            var first = InspectionLayout.resolve(viewport(size, 1), true);
            for (double scale : new double[] {2, 3, 4}) {
                var current = InspectionLayout.resolve(viewport(size, scale), true);
                assertEquals(first, current);
                assertEquals(first.modelSize(), current.renderScale(viewport(size, scale), 1, 1) * scale, 1e-8);
            }
        }
    }

    @Test void descriptionMovesBesideTheModelOnWideScreensAndBelowOnNarrowScreens() {
        var wide = InspectionLayout.resolve(viewport(new int[] {1920,1080}, 4), true);
        assertTrue(wide.sideColumn());
        assertTrue(wide.modelX() + wide.modelSize() / 2 < wide.textX());
        var narrow = InspectionLayout.resolve(viewport(new int[] {900,1200}, 3), true);
        assertFalse(narrow.sideColumn());
        assertTrue(narrow.modelY() + narrow.modelSize() / 2 < narrow.textY());
        assertTrue(narrow.textY() + narrow.textHeight() <= 1200 * 0.75);
    }

    @Test void absentDescriptionPreservesTheExistingItemCenterAndSize() {
        var viewport = viewport(new int[] {1920,1080}, 4);
        var layout = InspectionLayout.resolve(viewport, false);
        assertEquals(viewport.centerX(), layout.modelX() / 4);
        assertEquals(viewport.centerY(), layout.modelY() / 4);
        assertEquals(viewport.modelScale(), layout.renderScale(viewport, 1, 1));
        assertFalse(layout.hasDescription());
    }

    @Test void extremeAuthoredValuesRemainFinite() {
        var viewport = viewport(new int[] {1920,1080}, 4);
        var layout = InspectionLayout.resolve(viewport, true);
        assertTrue(Double.isFinite(layout.renderScale(viewport, Double.MAX_VALUE, Double.MAX_VALUE)));
    }

    @Test void narrowTextEndsBeforeTheReadingHintAtTheMaximumGuiScale() {
        for (int[] size : new int[][] {{854,480}, {900,1200}, {1280,720}}) {
            int scale = Math.max(1, Math.min(size[0] / 320, size[1] / 240));
            var layout = InspectionLayout.resolve(viewport(size, scale), true);
            double firstHint = (Math.ceil(size[1] / (double) scale) - 55) * scale;
            assertTrue(layout.textY() + layout.textHeight() < firstHint);
        }
    }

    private InspectionViewport viewport(int[] size, double scale) {
        return new InspectionViewport(size[0], size[1], scale,
                (int) Math.ceil(size[0] / scale), (int) Math.ceil(size[1] / scale));
    }
}
