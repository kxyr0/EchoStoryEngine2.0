package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtConnectionTraceTest {
    @Test void placesResultOnDrawnPathRatherThanPerpendicularOffset() {
        var path = List.of(new CanvasPoint(347, 124), new CanvasPoint(263, 136),
                new CanvasPoint(183, 344), new CanvasPoint(183, 380), new CanvasPoint(218, 438));
        var trace = ThoughtConnectionTrace.split(path);
        assertTrue(trace.result().x() < 263, "Result stays on the left-hand gesture instead of appearing on the right");
        assertEquals(length(trace.first()), length(trace.second()), 0.00001);
        assertEquals(trace.result(), trace.second().get(0));
        List<CanvasPoint> reconstructed = new ArrayList<>(trace.first());
        reconstructed.addAll(trace.second().subList(1, trace.second().size()));
        assertEquals(length(path), length(reconstructed), 0.00001);
        assertTrue(reconstructed.containsAll(path));
    }

    @Test void retainsAcceptedBendsEvenWhenStraightShortcutIsAvailable() {
        var path = List.of(new CanvasPoint(347, 124), new CanvasPoint(263, 136),
                new CanvasPoint(183, 344), new CanvasPoint(218, 438));
        assertEquals(path, new ThoughtConnectionRouter().route(path.get(0), path.get(3), List.of(), 18, path));
    }

    @Test void newResultLabelOnlyLocallyDetoursOriginalLeftRoute() {
        var path = List.of(new CanvasPoint(347, 124), new CanvasPoint(263, 136),
                new CanvasPoint(183, 344), new CanvasPoint(183, 380), new CanvasPoint(218, 438));
        var trace = ThoughtConnectionTrace.split(path);
        var boxes = List.of(new Box(100, 300, 340, 320), new Box(192, 359, 460, 375));
        var branch = trace.second();
        var route = new ThoughtConnectionRouter().route(branch.get(0), branch.get(branch.size() - 1), boxes, 18, branch);
        assertFalse(route.isEmpty());
        for (var point : route) assertTrue(point.x() <= trace.result().x() + 1, "Must not flip to the right side");
        for (int index = 1; index < route.size(); index++) {
            assertFalse(ThoughtConnectionRoute.blocked(route.get(index - 1), route.get(index), boxes));
        }
    }

    @Test void splitsExactMidpointAndZeroLengthPaths() {
        var trace = ThoughtConnectionTrace.split(List.of(new CanvasPoint(0, 0), new CanvasPoint(0, 10), new CanvasPoint(10, 10)));
        assertEquals(new CanvasPoint(0, 10), trace.result());
        assertEquals(List.of(new CanvasPoint(0, 10), new CanvasPoint(10, 10)), trace.second());
        assertEquals(new CanvasPoint(0, 0), ThoughtConnectionTrace.split(List.of(new CanvasPoint(0, 0), new CanvasPoint(0, 0))).result());
    }

    @Test void resultCaptionBesideVerticalGestureKeepsBothAcceptedBranchesExact() {
        var path = List.of(new CanvasPoint(347, 124), new CanvasPoint(263, 136),
                new CanvasPoint(183, 344), new CanvasPoint(183, 380), new CanvasPoint(218, 438));
        var trace = ThoughtConnectionTrace.split(path);
        var node = trace.result();
        double width = 208, height = 10, gap = 5, radius = 10;
        var circles = List.of(path.get(0), path.get(4), new CanvasPoint(329, 336), node);
        List<Box> boxes = new ArrayList<>(List.of(new Box(265, 146, 430, 166), new Box(186, 353, 464, 373)));
        var position = new ThoughtLabelLayout().place(new CanvasPoint(node.x() + width / 2 + radius + gap + 1,
                node.y() - height / 2), width, height, gap, node, radius, List.of(), circles, boxes);
        var center = new CanvasPoint(position.x(), position.y() + height / 2);
        for (var circle : circles) assertFalse(ThoughtLabelLayout.overlapsCircle(center, width / 2 + gap, height / 2 + gap, circle, radius));
        boxes.add(new Box(position.x() - width / 2 - gap, position.y() - gap,
                position.x() + width / 2 + gap, position.y() + height + gap));
        for (var branch : List.of(trace.first(), trace.second())) {
            assertEquals(branch, new ThoughtConnectionRouter().route(branch.get(0), branch.get(branch.size() - 1), boxes, 18, branch));
        }
    }

    private static double length(List<CanvasPoint> path) {
        double length = 0;
        for (int index = 1; index < path.size(); index++) length += ThoughtConnectionRoute.length(path.get(index - 1), path.get(index));
        return length;
    }
}
