package io.github.kxyr0.echoobservation.forge.client.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import org.junit.jupiter.api.Test;

class ObservationProjectionTest {
    private final ObservationProjection projection = new ObservationProjection();

    @Test
    void activeCameraMatrixProjectsCenterInPhysicalFramebuffer() {
        var anchor = new ObservationAnchor("minecraft:overworld", 0, 0, 9,
                0.0D, 0.0D, 1.0D, ObservationFace.NORTH);
        var point = projection.project(anchor, camera(1920, 1080)).orElseThrow();
        assertEquals(960.0D, point.framebufferX(), 0.0001D);
        assertEquals(540.0D, point.framebufferY(), 0.0001D);
        assertEquals(10.0D, point.depth(), 0.0001D);
    }

    @Test
    void framebufferResizeChangesPixelsButPreservesNormalizedPosition() {
        var anchor = new ObservationAnchor("minecraft:overworld", 1, 0, 9,
                0.0D, 0.0D, 1.0D, ObservationFace.NORTH);
        var small = projection.project(anchor, camera(960, 540)).orElseThrow();
        var large = projection.project(anchor, camera(1920, 1080)).orElseThrow();
        assertEquals(small.ndcX(), large.ndcX(), 0.0001D);
        assertEquals(small.framebufferX() * 2.0D, large.framebufferX(), 0.0001D);
        assertTrue(large.framebufferX() > 960.0D);
    }

    private VisibilityCameraSnapshot camera(int width, int height) {
        double aspect = width / (double) height;
        double f = 1.0D / Math.tan(Math.toRadians(70.0D) / 2.0D);
        double near = 0.05D;
        double far = 1000.0D;
        double[] matrix = {f / aspect, 0, 0, 0, 0, f, 0, 0,
                0, 0, (far + near) / (near - far), -1,
                0, 0, 2 * far * near / (near - far), 0};
        var zero = new VisibilityVector3(0, 0, 0);
        var forward = new VisibilityVector3(0, 0, 1);
        return new VisibilityCameraSnapshot("test", VisibilityViewpoint.ACTIVE_CAMERA, zero, forward,
                new VisibilityVector3(0, 1, 0), new VisibilityVector3(1, 0, 0), 0, 0,
                new VisibilityProjectionMatrix(matrix), width, height, 1L, zero, forward, 0, 0);
    }
}
