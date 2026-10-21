package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtLabelLayoutTest {
    private final ThoughtLabelLayout layout = new ThoughtLabelLayout();
    private final CanvasPoint origin = new CanvasPoint(0, 30);
    private final CanvasPoint node = new CanvasPoint(0, 0);

    @Test void leavesClearTextInPlace() {
        assertEquals(origin, layout.place(origin, 100, 12, 6, node, 10, List.of()));
    }

    @Test void clearsVerticalLineWithPadding() {
        var segments = List.of(segment(0, 10, 0, 200));
        var result = layout.place(origin, 100, 12, 6, node, 10, segments);
        assertNotEquals(origin, result);
        assertClear(result, 100, 12, 6, segments);
    }

    @Test void keepsTextNearOwnerWhenMultipleCrossingsNeedMasking() {
        var segments = List.of(segment(-200, 30, 200, 30), segment(-90, -50, 90, 130),
                segment(30, -50, 30, 200));
        var result = layout.place(origin, 100, 34, 6, node, 10, segments);
        assertCirclesClear(result, 100, 34, 6, 10, List.of(node));
        assertTrue(Math.abs(result.x()) <= 22);
        assertTrue(Math.abs(result.y() + 17) <= 59);
    }

    @Test void respectsSegmentEndpointsInsteadOfInfiniteLines() {
        assertEquals(origin, layout.place(origin, 100, 12, 6, node, 10,
                List.of(segment(0, 100, 0, 200))));
        assertFalse(ThoughtLabelLayout.intersects(new CanvasPoint(0, 0), 10, 10,
                segment(9, 15, 15, 9)));
        assertTrue(ThoughtLabelLayout.intersects(new CanvasPoint(0, 0), 10, 10,
                segment(10, -20, 10, 20)));
    }

    @Test void clearsWrappedTextAtDifferentScales() {
        for (double scale : new double[] {0.5, 1, 3}) {
            var segments = List.of(segment(-150 * scale, 0, 150 * scale, 100 * scale));
            var result = layout.place(new CanvasPoint(0, 30 * scale), 100 * scale, 34 * scale,
                    4 * scale + 2, node, 10 * scale, segments);
            assertClear(result, 100 * scale, 34 * scale, 4 * scale + 2, segments);
        }
    }

    @Test void avoidsOtherCircleEvenWithoutConnections() {
        var circles = List.of(node, new CanvasPoint(0, 36));
        var result = layout.place(origin, 100, 12, 6, node, 10, List.of(), circles);
        assertTrue(result.y() < node.y());
        assertCirclesClear(result, 100, 12, 6, 10, circles);
    }

    @Test void doesNotEscapeToDistantEndpointWhenNearbyTextNeedsLineMasking() {
        var circles = List.of(node, new CanvasPoint(0, -36));
        var segments = List.of(segment(0, -500, 0, 500));
        var result = layout.place(origin, 300, 12, 6, node, 10, segments, circles);
        assertEquals(origin, result);
        assertCirclesClear(result, 300, 12, 6, 10, circles);
    }

    @Test void avoidsNeighborAfterMovingAwayFromDiagonalConnectionAtEveryScale() {
        for (double scale : new double[] {0.5, 1, 3}) {
            var circles = List.of(node, new CanvasPoint(-111 * scale, 133 * scale));
            var segments = List.of(segment(0, 0, -111 * scale, 133 * scale));
            double gap = 4 * scale + 2;
            var result = layout.place(new CanvasPoint(0, 22 * scale), 270 * scale, 12 * scale,
                    gap, node, 10 * scale, segments, circles);
            assertCirclesClear(result, 270 * scale, 12 * scale, gap, 10 * scale, circles);
            assertTrue(Math.abs(result.x()) <= 10 * scale + 2 * gap);
            assertTrue(Math.abs(result.y()) < 60 * scale + 2 * gap);
        }
    }

    private static void assertCirclesClear(CanvasPoint position, double width, double height, double gap,
            double radius, List<CanvasPoint> circles) {
        var center = new CanvasPoint(position.x(), position.y() + height / 2);
        for (var circle : circles) {
            assertFalse(ThoughtLabelLayout.overlapsCircle(center, width / 2 + gap, height / 2 + gap, circle, radius));
        }
    }

    private static ThoughtLabelLayout.Segment segment(double x1, double y1, double x2, double y2) {
        return new ThoughtLabelLayout.Segment(new CanvasPoint(x1, y1), new CanvasPoint(x2, y2));
    }

    private static void assertClear(CanvasPoint position, double width, double height, double gap,
            List<ThoughtLabelLayout.Segment> segments) {
        CanvasPoint center = new CanvasPoint(position.x(), position.y() + height / 2);
        for (var segment : segments) {
            assertFalse(ThoughtLabelLayout.intersects(center, width / 2 + gap, height / 2 + gap, segment));
        }
    }
}
