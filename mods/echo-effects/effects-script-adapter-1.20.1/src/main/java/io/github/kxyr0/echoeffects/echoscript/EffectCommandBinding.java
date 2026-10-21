package io.github.kxyr0.echoeffects.echoscript;

import io.github.kxyr0.echoeffects.api.identity.EffectId;
import java.util.Objects;
import java.util.Optional;

public record EffectCommandBinding(
        EffectCommandKind kind,
        EffectId effectId,
        double intensity,
        Optional<Long> fadeOutTicks
) {
    public EffectCommandBinding {
        kind = Objects.requireNonNull(kind, "kind");
        effectId = Objects.requireNonNull(effectId, "effectId");
        fadeOutTicks = Objects.requireNonNull(fadeOutTicks, "fadeOutTicks");
        if (!Double.isFinite(intensity) || intensity <= 0.0D) {
            throw new IllegalArgumentException("Intensity must be positive and finite.");
        }
    }
}
