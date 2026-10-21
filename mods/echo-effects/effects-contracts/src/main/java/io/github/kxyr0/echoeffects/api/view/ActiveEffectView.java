package io.github.kxyr0.echoeffects.api.view;

import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.identity.EffectInstanceId;
import io.github.kxyr0.echoeffects.api.status.EffectOutcome;
import io.github.kxyr0.echoeffects.api.status.EffectStatus;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.api.time.EffectDuration;
import java.util.Objects;
import java.util.Optional;

public record ActiveEffectView(
        EffectInstanceId instanceId,
        EffectId effectId,
        EffectTargetRef target,
        EffectStatus status,
        Optional<EffectOutcome> outcome,
        long elapsedTicks,
        EffectDuration duration,
        double intensity
) {
    public ActiveEffectView {
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        effectId = Objects.requireNonNull(effectId, "effectId");
        target = Objects.requireNonNull(target, "target");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        duration = Objects.requireNonNull(duration, "duration");
        if (elapsedTicks < 0L || !Double.isFinite(intensity) || intensity <= 0.0D) {
            throw new IllegalArgumentException("Invalid active effect view values.");
        }
    }
}
