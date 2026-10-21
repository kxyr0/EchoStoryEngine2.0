package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import java.util.Objects;

public record EffectApplyRequest(
        EffectTargetRef target,
        EffectId effectId,
        EffectCommandId commandId,
        double intensity
) {
    public EffectApplyRequest {
        target = Objects.requireNonNull(target, "target");
        effectId = Objects.requireNonNull(effectId, "effectId");
        commandId = Objects.requireNonNull(commandId, "commandId");
        if (!Double.isFinite(intensity) || intensity <= 0.0D) {
            throw new IllegalArgumentException("Intensity must be positive and finite.");
        }
    }
}
