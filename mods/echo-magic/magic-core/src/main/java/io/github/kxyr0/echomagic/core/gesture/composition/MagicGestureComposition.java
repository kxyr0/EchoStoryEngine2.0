package io.github.kxyr0.echomagic.core.gesture.composition;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import java.util.Objects;

public record MagicGestureComposition(MagicTechnique technique, MagicGestureModifier modifier) {
    public MagicGestureComposition {
        technique = Objects.requireNonNull(technique, "technique");
        modifier = Objects.requireNonNull(modifier, "modifier");
    }
}
