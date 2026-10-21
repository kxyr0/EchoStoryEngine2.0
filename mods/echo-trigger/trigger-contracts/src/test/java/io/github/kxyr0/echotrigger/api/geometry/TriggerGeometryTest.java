package io.github.kxyr0.echotrigger.api.geometry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TriggerGeometryTest {
    @Test
    void boxUsesInclusiveMinAndExclusiveMax() {
        BoxArea box = new BoxArea(new Vector3(0, 0, 0), new Vector3(2, 2, 2));

        assertTrue(box.contains(new Vector3(0, 0, 0)));
        assertFalse(box.contains(new Vector3(2, 1, 1)));
    }

    @Test
    void sphereContainsRadiusBoundary() {
        SphereArea sphere = new SphereArea(new Vector3(0, 0, 0), 3);

        assertTrue(sphere.contains(new Vector3(3, 0, 0)));
        assertFalse(sphere.contains(new Vector3(3.1D, 0, 0)));
    }

    @Test
    void cylinderChecksRadiusAndVerticalRange() {
        CylinderArea cylinder = new CylinderArea(new Vector3(0, 0, 0), 2, 64, 68);

        assertTrue(cylinder.contains(new Vector3(1, 65, 1)));
        assertFalse(cylinder.contains(new Vector3(0, 68, 0)));
    }

    @Test
    void polygonRejectsSelfIntersection() {
        List<Vector3> points = List.of(
                new Vector3(0, 64, 0),
                new Vector3(2, 64, 2),
                new Vector3(0, 64, 2),
                new Vector3(2, 64, 0));

        assertThrows(IllegalArgumentException.class, () -> new PolygonArea(points, 64, 68));
    }

    @Test
    void polygonContainsEdgesAndInterior() {
        PolygonArea polygon = new PolygonArea(List.of(
                new Vector3(0, 64, 0),
                new Vector3(4, 64, 0),
                new Vector3(4, 64, 4),
                new Vector3(0, 64, 4)), 64, 68);

        assertTrue(polygon.contains(new Vector3(2, 65, 2)));
        assertTrue(polygon.contains(new Vector3(0, 65, 2)));
        assertFalse(polygon.contains(new Vector3(5, 65, 2)));
    }
}
