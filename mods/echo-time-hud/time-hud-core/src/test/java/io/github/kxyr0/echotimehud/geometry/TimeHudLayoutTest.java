package io.github.kxyr0.echotimehud.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotimehud.api.HudAnchor;
import io.github.kxyr0.echotimehud.api.HudConfig;
import io.github.kxyr0.echotimehud.api.HudScreenPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeHudLayoutTest {
    private final TimeHudLayout layout = new TimeHudLayout();

    @Test void topRightUsesPhysicalReferenceCanvasAtEveryGuiScale() {
        HudConfig config = config(HudAnchor.TOP_RIGHT, -24, 24, true);
        HudLayoutResult first = layout.place(new HudLayoutInput(3840, 2160, 30, 9, config));
        HudLayoutResult second = layout.place(new HudLayoutInput(3840, 2160, 30, 9, config));
        assertEquals(first.finalTextRect(), second.finalTextRect());
        assertEquals(2.0, first.uniformScale());
        assertEquals(first.finalTextRect().x() / 4.0, first.guiX(4.0));
    }

    @Test void allNineAnchorsRemainInSafeRect() {
        for (HudAnchor anchor : HudAnchor.values()) {
            HudLayoutResult result = layout.place(new HudLayoutInput(2560, 1080, 35, 9,
                    config(anchor, 0, 0, true)));
            assertTrue(result.finalTextRect().x() >= result.safeRect().x());
            assertTrue(result.finalTextRect().right() <= result.safeRect().right());
            assertTrue(result.finalTextRect().y() >= result.safeRect().y());
            assertTrue(result.finalTextRect().bottom() <= result.safeRect().bottom());
        }
    }

    @Test void clampContainsExtremeOffsets() {
        HudLayoutResult result = layout.place(new HudLayoutInput(800, 600, 30, 9,
                config(HudAnchor.BOTTOM_RIGHT, 5000, -5000, true)));
        assertEquals(result.safeRect().right(), result.finalTextRect().right(), 0.0001);
        assertEquals(result.safeRect().y(), result.finalTextRect().y(), 0.0001);
    }

    @Test void rightEdgeIsStableAcrossChangingTextWidth() {
        HudConfig config = config(HudAnchor.TOP_RIGHT, 0, 0, true);
        HudLayoutResult narrow = layout.place(new HudLayoutInput(1920, 1080, 24, 9, config));
        HudLayoutResult wide = layout.place(new HudLayoutInput(1920, 1080, 31, 9, config));
        assertEquals(narrow.finalTextRect().right(), wide.finalTextRect().right(), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({"1920,1080", "1280,720", "2560,1440", "3440,1440", "720,1280"})
    void referenceFitHandlesTargetResolutionsAndPortrait(int width, int height) {
        HudLayoutResult result = layout.place(new HudLayoutInput(width, height, 30, 9,
                config(HudAnchor.TOP_RIGHT, -24, 24, true)));
        assertEquals(Math.min((double) width / 1920, (double) height / 1080), result.uniformScale(), 0.0001);
        assertTrue(result.finalTextRect().x() >= result.safeRect().x());
        assertTrue(result.finalTextRect().right() <= result.safeRect().right());
    }

    private HudConfig config(HudAnchor anchor, double x, double y, boolean clamp) {
        return new HudConfig("jdh:test", true, anchor, x, y, 1920, 1080, 12, 1, "HH:mm",
                false, true, false, 1, true, clamp, HudScreenPolicy.SHOW_DURING_GAME_SCREENS, 1);
    }
}
