package io.github.kxyr0.echovisibility.core.occlusion;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;

@FunctionalInterface
public interface VisibilityOcclusionProbe {
    VisibilityOcclusionResult probe(VisibilityVector3 origin, VisibilityVector3 sample);

    static VisibilityOcclusionProbe noOcclusion() {
        return (origin, sample) -> VisibilityOcclusionResult.clear();
    }
}
