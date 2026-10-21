package io.github.kxyr0.echodebugvisibilityadapter.forge.client;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4d;
import org.joml.Vector4d;

public final class VisibilityFrustumDebugGeometry {
    private static final double NEAR_DISTANCE = 0.1D;
    private static final double FAR_DISTANCE = 24.0D;

    public List<Segment> segments(VisibilityCameraSnapshot camera) {
        Matrix4d inverse = new Matrix4d().set(camera.projection().values()).invert();
        VisibilityVector3[] near = corners(camera, inverse, NEAR_DISTANCE);
        VisibilityVector3[] far = corners(camera, inverse, FAR_DISTANCE);
        List<Segment> result = new ArrayList<>();
        addLoop(result, near);
        addLoop(result, far);
        for (int index = 0; index < 4; index++) {
            result.add(new Segment(near[index], far[index]));
        }
        return List.copyOf(result);
    }

    private static VisibilityVector3[] corners(
            VisibilityCameraSnapshot camera,
            Matrix4d inverse,
            double distance) {
        return new VisibilityVector3[] {
            corner(camera, inverse, -1.0D, -1.0D, distance),
            corner(camera, inverse, 1.0D, -1.0D, distance),
            corner(camera, inverse, 1.0D, 1.0D, distance),
            corner(camera, inverse, -1.0D, 1.0D, distance)
        };
    }

    private static VisibilityVector3 corner(
            VisibilityCameraSnapshot camera,
            Matrix4d inverse,
            double x,
            double y,
            double distance) {
        Vector4d view = inverse.transform(new Vector4d(x, y, 1.0D, 1.0D));
        view.div(view.w);
        VisibilityVector3 direction = camera.right().scale(view.x)
                .add(camera.up().scale(view.y))
                .add(camera.forward().scale(-view.z)).normalized();
        return camera.position().add(direction.scale(distance));
    }

    private static void addLoop(List<Segment> result, VisibilityVector3[] points) {
        for (int index = 0; index < 4; index++) {
            result.add(new Segment(points[index], points[(index + 1) % 4]));
        }
    }

    public record Segment(VisibilityVector3 from, VisibilityVector3 to) {
    }
}
