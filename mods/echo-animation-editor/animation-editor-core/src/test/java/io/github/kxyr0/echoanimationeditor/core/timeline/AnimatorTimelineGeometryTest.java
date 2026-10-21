package io.github.kxyr0.echoanimationeditor.core.timeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnimatorTimelineGeometryTest {
    @Test
    void frameAndPixelMappingClampToClipBounds() {
        AnimatorTimelineGeometry timeline = new AnimatorTimelineGeometry(120, 20, 620);

        assertEquals(20, timeline.frameToX(-5));
        assertEquals(620, timeline.frameToX(200));
        assertEquals(60, timeline.xToFrame(320));
        assertEquals(0, timeline.xToFrame(-100));
        assertEquals(120, timeline.xToFrame(900));
    }
}
