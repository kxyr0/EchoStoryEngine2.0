package io.github.kxyr0.echothoughtspace.forge.client.input;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import java.util.Objects;

public record ThoughtConnectionVisual(
        CanvasPoint source,
        CanvasPoint endpoint,
        double opacity,
        boolean validTarget
) {
    public ThoughtConnectionVisual {
        source = Objects.requireNonNull(source, "source");
        endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (!Double.isFinite(opacity) || opacity < 0.0D || opacity > 1.0D) {
            throw new IllegalArgumentException("Connection opacity must be within 0..1.");
        }
    }
}
