package io.github.kxyr0.echoobservation.forge.client.projection;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import java.util.Optional;

public final class ObservationProjection {
    public Optional<ObservationScreenPoint> project(ObservationAnchor anchor, VisibilityCameraSnapshot camera) {
        VisibilityVector3 delta = new VisibilityVector3(anchor.worldX() - camera.position().x(),
                anchor.worldY() - camera.position().y(), anchor.worldZ() - camera.position().z());
        double depth = delta.dot(camera.forward());
        if (!Double.isFinite(depth) || depth <= 0.001D) return Optional.empty();
        double[] view = {delta.dot(camera.right()), delta.dot(camera.up()), -depth, 1.0D};
        double[] clip = multiply(camera.projection(), view);
        if (!Double.isFinite(clip[3]) || Math.abs(clip[3]) < 0.000001D) return Optional.empty();
        double ndcX = clip[0] / clip[3];
        double ndcY = clip[1] / clip[3];
        double ndcZ = clip[2] / clip[3];
        if (!Double.isFinite(ndcX) || !Double.isFinite(ndcY) || !Double.isFinite(ndcZ)
                || Math.abs(ndcX) > 1.0D || Math.abs(ndcY) > 1.0D || ndcZ < -1.0D || ndcZ > 1.0D) {
            return Optional.empty();
        }
        double x = (ndcX * 0.5D + 0.5D) * camera.framebufferWidth();
        double y = (0.5D - ndcY * 0.5D) * camera.framebufferHeight();
        return Optional.of(new ObservationScreenPoint(x, y, depth, ndcX, ndcY));
    }

    private double[] multiply(VisibilityProjectionMatrix matrix, double[] vector) {
        double[] result = new double[4];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                result[row] += matrix.value(column, row) * vector[column];
            }
        }
        return result;
    }
}
