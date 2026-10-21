package io.github.kxyr0.echonewui.core.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PhysicalToGuiMapperTest {
    private static final UiLayoutConfig CONFIG = UiLayoutConfig.defaults();
    private final JournalBookLayout layout = new JournalBookLayout();
    private final PhysicalToGuiMapper mapper = new PhysicalToGuiMapper();

    @Test
    void guiScaleDoesNotChangePhysicalBookRectangle() {
        UiRectangle expected = new UiRectangle(568, 234, 784, 612);
        int[][] guiSizes = {
                {1920, 1080, 1},
                {960, 540, 2},
                {640, 360, 3},
                {480, 270, 4},
                {640, 360, 0}
        };

        for (int[] gui : guiSizes) {
            UiViewport viewport = new UiViewport(1920, 1080, gui[0], gui[1], scale(gui[2]), gui[2] + 1L);
            JournalBookLayoutResult result = layout.layout(viewport, CONFIG);

            assertEquals(expected, result.physicalRect());
        }
    }

    @Test
    void guiMappingCanRoundTripToPhysicalEdgesWithinOnePixel() {
        UiViewport viewport = new UiViewport(1920, 1080, 640, 360, 3.0D, 1L);
        JournalBookLayoutResult result = layout.layout(viewport, CONFIG);
        UiRectangle roundTrip = mapper.backToPhysical(result.guiRect(), viewport);

        assertEdgeClose(result.physicalRect().x(), roundTrip.x());
        assertEdgeClose(result.physicalRect().y(), roundTrip.y());
        assertEdgeClose(result.physicalRect().right(), roundTrip.right());
        assertEdgeClose(result.physicalRect().bottom(), roundTrip.bottom());
    }

    private static double scale(int guiScale) {
        return guiScale == 0 ? 3.0D : guiScale;
    }

    private static void assertEdgeClose(int expected, int actual) {
        assertTrue(Math.abs(expected - actual) <= 1, "expected=" + expected + " actual=" + actual);
    }
}
