package io.github.kxyr0.echovisibility.core.observation;

import io.github.kxyr0.echovisibility.core.math.VisibilityClipPoint;
import io.github.kxyr0.echovisibility.core.occlusion.VisibilityOcclusionResult;
import io.github.kxyr0.echovisibility.core.sample.VisibilitySamplePoint;
import java.util.Objects;

public record VisibilityEvaluatedSample(
        VisibilitySamplePoint sample,
        VisibilityClipPoint clip,
        boolean inViewport,
        VisibilityOcclusionResult occlusion) {
    public VisibilityEvaluatedSample {
        sample = Objects.requireNonNull(sample, "sample");
        clip = Objects.requireNonNull(clip, "clip");
        occlusion = Objects.requireNonNull(occlusion, "occlusion");
    }
}
