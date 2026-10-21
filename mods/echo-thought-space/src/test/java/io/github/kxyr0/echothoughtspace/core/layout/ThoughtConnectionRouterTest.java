package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtConnectionRouterTest {
    private final ThoughtConnectionRouter router = new ThoughtConnectionRouter();

    @Test void keepsUnobstructedLineStraight() {
        var start = new CanvasPoint(0, 0);
        var end = new CanvasPoint(200, 0);
        assertEquals(List.of(start, end), router.route(start, end, List.of(new Box(40, 30, 80, 60)), 18));
    }

    @Test void smoothlyRoutesAcrossWideLabelAndKeepsEndpoints() {
        var start = new CanvasPoint(0, -100);
        var end = new CanvasPoint(0, 100);
        var boxes = List.of(new Box(-100, -12, 100, 12));
        var path = router.route(start, end, boxes, 18);
        assertEquals(start, path.get(0));
        assertEquals(end, path.get(path.size() - 1));
        assertTrue(path.size() > 10);
        assertClear(path, boxes);
        assertEquals(path, router.route(start, end, boxes, 18));
        for (int index = 2; index < path.size(); index++) {
            var a = path.get(index - 2); var b = path.get(index - 1); var c = path.get(index);
            double cosine = ((b.x() - a.x()) * (c.x() - b.x()) + (b.y() - a.y()) * (c.y() - b.y()))
                    / (ThoughtConnectionRoute.length(a, b) * ThoughtConnectionRoute.length(b, c));
            assertTrue(cosine > 0.95, "Bends must be rounded rather than sharp corners");
        }
    }

    @Test void avoidsSeveralAndOverlappingLabelsAtDifferentZooms() {
        for (double scale : new double[] {0.5, 1, 3}) {
            var boxes = List.of(new Box(-60 * scale, -40 * scale, 30 * scale, 0),
                    new Box(-20 * scale, -10 * scale, 70 * scale, 30 * scale),
                    new Box(-80 * scale, 50 * scale, 40 * scale, 70 * scale));
            var path = router.route(new CanvasPoint(0, -100 * scale), new CanvasPoint(0, 130 * scale), boxes, 18 * scale);
            assertTrue(path.size() > 2);
            assertClear(path, boxes);
        }
    }

    @Test void stopsDraggedLineAtTextBoundaryWhenPointerIsInsideLabel() {
        var boxes = List.of(new Box(-30, -10, 30, 10));
        var path = router.route(new CanvasPoint(0, -100), new CanvasPoint(0, 0), boxes, 18);
        assertClear(path, boxes);
        assertFalse(boxes.get(0).contains(path.get(path.size() - 1)));
    }

    @Test void handlesDegenerateLineAndEndpointNearText() {
        var point = new CanvasPoint(0, 0);
        assertEquals(List.of(point, point), router.route(point, point, List.of(), 18));
        var boxes = List.of(new Box(-100, 11, 100, 30));
        assertClear(router.route(new CanvasPoint(0, 10), new CanvasPoint(0, 200), boxes, 18), boxes);
    }

    private static void assertClear(List<CanvasPoint> path, List<Box> boxes) {
        assertFalse(path.isEmpty());
        for (int index = 1; index < path.size(); index++) {
            assertFalse(ThoughtConnectionRoute.blocked(path.get(index - 1), path.get(index), boxes));
        }
    }
}
