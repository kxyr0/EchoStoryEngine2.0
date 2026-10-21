package io.github.kxyr0.echovisibility.core.math;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;

public final class VisibilityProjection {
    public VisibilityClipPoint project(VisibilityCameraSnapshot camera, VisibilityVector3 world) {
        VisibilityVector3 offset = world.subtract(camera.position());
        double viewX = offset.dot(camera.right());
        double viewY = offset.dot(camera.up());
        double viewZ = -offset.dot(camera.forward());
        return multiply(camera.projection(), viewX, viewY, viewZ);
    }

    private static VisibilityClipPoint multiply(
            VisibilityProjectionMatrix matrix,
            double x,
            double y,
            double z) {
        double clipX = matrix.value(0, 0) * x + matrix.value(1, 0) * y
                + matrix.value(2, 0) * z + matrix.value(3, 0);
        double clipY = matrix.value(0, 1) * x + matrix.value(1, 1) * y
                + matrix.value(2, 1) * z + matrix.value(3, 1);
        double clipZ = matrix.value(0, 2) * x + matrix.value(1, 2) * y
                + matrix.value(2, 2) * z + matrix.value(3, 2);
        double clipW = matrix.value(0, 3) * x + matrix.value(1, 3) * y
                + matrix.value(2, 3) * z + matrix.value(3, 3);
        return new VisibilityClipPoint(clipX, clipY, clipZ, clipW);
    }
}
