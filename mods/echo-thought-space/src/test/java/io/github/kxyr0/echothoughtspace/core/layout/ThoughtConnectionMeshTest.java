package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtConnectionMeshTest {
    @Test void facesCameraLikeGuiFillForEveryLineDirection() {
        for (int angle = 0; angle < 360; angle += 15) {
            double radians = Math.toRadians(angle);
            var mesh = ThoughtConnectionMesh.build(List.of(new CanvasPoint(0, 0),
                    new CanvasPoint(100 * Math.cos(radians), 100 * Math.sin(radians))));
            assertEquals(1, mesh.size());
            assertTrue(signedArea(mesh.get(0)) < 0, "GUI front faces require clockwise screen winding");
            assertEquals(3, ThoughtConnectionRoute.length(mesh.get(0).first(), mesh.get(0).fourth()), 0.0001);
        }
    }

    @Test void roundedRouteIsVisibleAndHasNoGapsBetweenQuads() {
        var path = new ThoughtConnectionRouter().route(new CanvasPoint(0, -100), new CanvasPoint(0, 100),
                List.of(new Box(-100, -12, 100, 12)), 18);
        var mesh = ThoughtConnectionMesh.build(path);
        assertFalse(mesh.isEmpty());
        for (int index = 0; index < mesh.size(); index++) {
            assertTrue(signedArea(mesh.get(index)) < 0);
            if (index > 0) {
                assertEquals(mesh.get(index - 1).second(), mesh.get(index).first());
                assertEquals(mesh.get(index - 1).third(), mesh.get(index).fourth());
            }
        }
    }

    private static double signedArea(ThoughtConnectionMesh.Quad quad) {
        var points = List.of(quad.first(), quad.second(), quad.third(), quad.fourth());
        double area = 0;
        for (int index = 0; index < 4; index++) {
            CanvasPoint a = points.get(index), b = points.get((index + 1) % 4);
            area += a.x() * b.y() - b.x() * a.y();
        }
        return area / 2;
    }
}
