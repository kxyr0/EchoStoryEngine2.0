package io.github.kxyr0.echobackgroundnpc.core.render;

import java.util.Optional;
import java.util.function.Supplier;

/** Ensures frustum visibility is decided before expensive pose evaluation. */
public final class AnimationVisibilityGate {
    public <T> Optional<T> sample(boolean insideFrustum, Supplier<T> sampler) {
        return insideFrustum ? Optional.of(sampler.get()) : Optional.empty();
    }
}
