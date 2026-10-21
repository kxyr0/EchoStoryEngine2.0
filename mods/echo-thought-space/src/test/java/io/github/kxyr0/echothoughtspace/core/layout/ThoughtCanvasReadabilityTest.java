package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtCanvasReadabilityTest {
    @Test void screenshotNeighborsDoNotOverlapInEitherPlacementOrderOrZoom() {
        for (double scale : new double[] {0.5, 1, 2}) {
            for (var order : List.of(List.of(0, 1, 2, 3), List.of(3, 2, 1, 0))) {
                var circles = List.of(new CanvasPoint(623 * scale, 133 * scale), new CanvasPoint(591 * scale, 503 * scale),
                        new CanvasPoint(705 * scale, 487 * scale), new CanvasPoint(398 * scale, 680 * scale));
                double[] widths = {268, 468, 360, 364};
                List<Box> occupied = new ArrayList<>();
                var layout = new ThoughtLabelLayout();
                for (int index : order) {
                    CanvasPoint node = circles.get(index);
                    double width = widths[index] * scale, height = 20 * scale, gap = 10 * scale;
                    var origin = new CanvasPoint(node.x(), node.y() + 41 * scale);
                    var position = layout.place(origin, width, height, gap, node, 20 * scale, List.of(), circles, occupied);
                    var center = new CanvasPoint(position.x(), position.y() + height / 2);
                    for (var circle : circles) {
                        assertFalse(ThoughtLabelLayout.overlapsCircle(center, width / 2 + gap, height / 2 + gap, circle, 20 * scale));
                    }
                    for (var label : occupied) {
                        assertFalse(ThoughtLabelLayout.overlapsBox(center, width / 2 + gap, height / 2 + gap, label));
                    }
                    assertTrue(Math.abs(position.y() - node.y()) < 100 * scale, "Text must stay near its own circle");
                    occupied.add(new Box(position.x() - width / 2 - gap, position.y() - gap,
                            position.x() + width / 2 + gap, position.y() + height + gap));
                }
                for (int input : new int[] {0, 1}) {
                    var path = new ThoughtConnectionRouter().route(circles.get(input), circles.get(2), occupied, 18 * scale);
                    assertTrue(path.size() >= 2, "Connections must remain drawable");
                    assertFalse(ThoughtConnectionMesh.build(path).isEmpty());
                    for (int point = 1; point < path.size(); point++) {
                        assertFalse(ThoughtConnectionRoute.blocked(path.get(point - 1), path.get(point), occupied));
                    }
                }
            }
        }
    }
}
