package io.github.kxyr0.echovisibility.api.report;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import java.util.Objects;

public record VisibilityCameraSnapshot(
        String source,
        VisibilityViewpoint viewpoint,
        VisibilityVector3 position,
        VisibilityVector3 forward,
        VisibilityVector3 up,
        VisibilityVector3 right,
        double yaw,
        double pitch,
        VisibilityProjectionMatrix projection,
        int framebufferWidth,
        int framebufferHeight,
        long projectionRevision,
        VisibilityVector3 playerEyePosition,
        VisibilityVector3 playerForward,
        double playerYaw,
        double playerPitch) {
    public VisibilityCameraSnapshot {
        source = requireText(source, "source");
        viewpoint = Objects.requireNonNull(viewpoint, "viewpoint");
        position = Objects.requireNonNull(position, "position");
        forward = Objects.requireNonNull(forward, "forward").normalized();
        up = Objects.requireNonNull(up, "up").normalized();
        right = Objects.requireNonNull(right, "right").normalized();
        projection = Objects.requireNonNull(projection, "projection");
        playerEyePosition = Objects.requireNonNull(playerEyePosition, "playerEyePosition");
        playerForward = Objects.requireNonNull(playerForward, "playerForward").normalized();
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch)
                || !Double.isFinite(playerYaw) || !Double.isFinite(playerPitch)
                || framebufferWidth < 1 || framebufferHeight < 1 || projectionRevision < 0L) {
            throw new IllegalArgumentException("Camera snapshot contains invalid scalar values.");
        }
    }

    public double dynamicFovDegrees() {
        return projection.verticalFovDegrees();
    }

    public double aspectRatio() {
        return projection.aspectRatio();
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException(name + " must contain 1..128 characters.");
        }
        return checked;
    }
}
