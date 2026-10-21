package io.github.kxyr0.echomagic.forge.client.gesture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MagicGestureSmootherTest {
    @Test void preservesSharpTriangleCornerAndUnsmoothsForcedEndpoint() {
        MagicGestureSmoother smoother = new MagicGestureSmoother();
        smoother.filter(point(0.0D, 0.0D, 0L), true);
        MagicGesturePoint corner = point(0.5D, 1.0D, 40L);
        smoother.filter(corner, false);
        MagicGestureSmoother.Sample turn = smoother.filter(point(1.0D, 0.0D, 80L), false);
        assertNotNull(turn.corner());
        assertEquals(corner, turn.corner());
        MagicGesturePoint endpoint = point(1.2D, -0.4D, 120L);
        assertEquals(endpoint, smoother.filter(endpoint, true).point());
    }

    @Test void exposesRawPenPositionBeforeAStrokeStarts() {
        MagicGesturePlane plane = new MagicGesturePlane(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D),
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D));
        MagicGestureSession session = new MagicGestureSession(MagicGestureMode.SCHOOL,
                plane, Optional.empty(), false);

        session.preview(new MagicGesturePlane.Projection(0.37D, -0.21D, new Vec3(1.0D, 2.0D, 3.0D)));

        MagicGesturePoint cursor = session.cursor().orElseThrow();
        assertEquals(0.37D, cursor.x());
        assertEquals(-0.21D, cursor.y());
        assertEquals(new Vec3(1.0D, 2.0D, 3.0D), session.lastWorld().orElseThrow());
    }

    @Test void playerTranslationDoesNotMoveThePenAndProjectionStaysInsideCanvas() {
        MagicGesturePlane plane = new MagicGesturePlane(new Vec3(0.0D, 0.0D, 2.25D),
                new Vec3(0.0D, 0.0D, 1.0D), new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 direction = new Vec3(0.18D, 0.11D, 1.0D).normalize();
        MagicGesturePlane.Projection stationary = plane.project(Vec3.ZERO, direction).orElseThrow();
        MagicGesturePlane.Projection moved = plane.project(new Vec3(7.0D, 2.5D, -4.0D),
                direction).orElseThrow();

        assertEquals(stationary.x(), moved.x(), 1.0E-9D);
        assertEquals(stationary.y(), moved.y(), 1.0E-9D);
        MagicGesturePlane.Projection edge = plane.project(Vec3.ZERO,
                new Vec3(1.0D, 1.0D, 1.0D)).orElseThrow();
        assertTrue(Math.abs(edge.x()) <= MagicGesturePlane.CANVAS_HALF_WIDTH);
        assertTrue(Math.abs(edge.y()) <= MagicGesturePlane.CANVAS_HALF_HEIGHT);
        assertEquals(MagicGesturePlane.CANVAS_HALF_WIDTH, edge.x(), 1.0E-9D);
        assertEquals(MagicGesturePlane.CANVAS_HALF_HEIGHT, edge.y(), 1.0E-9D);
    }

    private static MagicGesturePoint point(double x, double y, long time) {
        return new MagicGesturePoint(x, y, time);
    }
}
