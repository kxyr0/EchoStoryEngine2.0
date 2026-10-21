package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Objects;

/**
 * Face layer draw command with evaluated model-space transform.
 */
public record ProjectedFaceLayer(LayerDrawCommand command, Transform3 transform) {
    public ProjectedFaceLayer {
        command = Objects.requireNonNull(command, "command");
        transform = Objects.requireNonNull(transform, "transform");
    }
}
