package io.github.kxyr0.echovisibility.core.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import org.junit.jupiter.api.Test;

class VisibilityFrustumClassifierTest {
    private final VisibilityFrustumClassifier classifier = new VisibilityFrustumClassifier();

    @Test
    void classifiesAheadBehindAndOutside() {
        assertEquals(VisibilityFrustumClassification.FULLY_INSIDE,
                classify(box(new Bounds(-0.5D, -0.5D, -6.0D, 0.5D, 0.5D, -5.0D)),
                        70.0D, 16.0D / 9.0D));
        assertEquals(VisibilityFrustumClassification.BEHIND_CAMERA,
                classify(box(new Bounds(-0.5D, -0.5D, 5.0D, 0.5D, 0.5D, 6.0D)),
                        70.0D, 16.0D / 9.0D));
        assertEquals(VisibilityFrustumClassification.FULLY_OUTSIDE,
                classify(box(new Bounds(20.0D, -0.5D, -6.0D, 21.0D, 0.5D, -5.0D)),
                        70.0D, 16.0D / 9.0D));
    }

    @Test
    void oneCornerAtScreenEdgeRemainsIntersecting() {
        VisibilityFrustumClassification result = classify(
                box(new Bounds(5.4D, -0.5D, -5.5D, 6.2D, 0.5D, -4.5D)), 70.0D, 16.0D / 9.0D);
        assertEquals(VisibilityFrustumClassification.INTERSECTING, result);
    }

    @Test
    void fovAndAspectChangeTheSameBoundsResult() {
        VisibilityAabb target = box(new Bounds(4.0D, -0.5D, -5.5D, 4.5D, 0.5D, -4.5D));
        assertNotEquals(classify(target, 40.0D, 1.0D), classify(target, 100.0D, 2.4D));
    }

    @Test
    void cameraInsideTargetIsIntersectingNotOutside() {
        assertEquals(VisibilityFrustumClassification.INTERSECTING,
                classify(box(new Bounds(-1.0D, -1.0D, -1.0D, 1.0D, 1.0D, 1.0D)), 70.0D, 1.0D));
    }

    private VisibilityFrustumClassification classify(VisibilityAabb bounds, double fov, double aspect) {
        return classifier.classify(camera(fov, aspect), bounds, 0).classification();
    }

    private static VisibilityCameraSnapshot camera(double fov, double aspect) {
        return new VisibilityCameraSnapshot("test", VisibilityViewpoint.ACTIVE_CAMERA,
                new VisibilityVector3(0.0D, 0.0D, 0.0D), new VisibilityVector3(0.0D, 0.0D, -1.0D),
                new VisibilityVector3(0.0D, 1.0D, 0.0D), new VisibilityVector3(1.0D, 0.0D, 0.0D),
                0.0D, 0.0D, perspective(fov, aspect, 0.05D, 100.0D), 1920, 1080, 1L,
                new VisibilityVector3(0.0D, 0.0D, 0.0D), new VisibilityVector3(0.0D, 0.0D, -1.0D),
                0.0D, 0.0D);
    }

    private static VisibilityProjectionMatrix perspective(double fov, double aspect, double near, double far) {
        double f = 1.0D / Math.tan(Math.toRadians(fov) * 0.5D);
        double[] matrix = new double[16];
        matrix[0] = f / aspect;
        matrix[5] = f;
        matrix[10] = (far + near) / (near - far);
        matrix[11] = -1.0D;
        matrix[14] = 2.0D * far * near / (near - far);
        return new VisibilityProjectionMatrix(matrix);
    }

    private static VisibilityAabb box(Bounds value) {
        return new VisibilityAabb(new VisibilityVector3(value.minX(), value.minY(), value.minZ()),
                new VisibilityVector3(value.maxX(), value.maxY(), value.maxZ()));
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }
}
