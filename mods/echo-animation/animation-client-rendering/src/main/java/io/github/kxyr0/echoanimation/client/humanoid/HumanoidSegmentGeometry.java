package io.github.kxyr0.echoanimation.client.humanoid;

import io.github.kxyr0.echoanimation.api.math.IntRect;
import java.util.Objects;

/**
 * Testable vanilla-skin geometry descriptor for one base or overlay segment.
 */
public record HumanoidSegmentGeometry(
        HumanoidSegmentId segmentId,
        double widthPixels,
        double heightPixels,
        double depthPixels,
        IntRect uv,
        boolean overlay
) {
    public HumanoidSegmentGeometry {
        segmentId = Objects.requireNonNull(segmentId, "segmentId");
        uv = Objects.requireNonNull(uv, "uv");
        if (widthPixels <= 0.0D || heightPixels <= 0.0D || depthPixels <= 0.0D) {
            throw new IllegalArgumentException("Segment dimensions must be positive.");
        }
    }
}
