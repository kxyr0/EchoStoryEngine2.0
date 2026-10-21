package io.github.kxyr0.echomagic.core.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SweptSegmentTest {
    @Test void highSpeedSegmentHitsThinExpandedTarget() {
        SweptSegment segment = new SweptSegment(new MagicPoint(0, 0, 0), new MagicPoint(1.6, 0, 0));
        var hit = segment.firstIntersection(new MagicAabb(0.79, -0.01, -0.01, 0.81, 0.01, 0.01).inflate(0.22));
        assertTrue(hit.isPresent());
        assertEquals(0.35625, hit.getAsDouble(), 0.00001);
    }
}

