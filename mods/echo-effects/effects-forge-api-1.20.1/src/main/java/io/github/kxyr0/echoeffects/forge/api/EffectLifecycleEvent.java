package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.identity.EffectInstanceId;
import io.github.kxyr0.echoeffects.api.status.EffectOutcome;
import io.github.kxyr0.echoeffects.api.status.EffectStatus;
import java.util.Objects;
import java.util.Optional;

public record EffectLifecycleEvent(
        EffectInstanceId instanceId,
        EffectStatus status,
        Optional<EffectOutcome> outcome,
        long serverTick
) {
    public EffectLifecycleEvent {
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (serverTick < 0L) {
            throw new IllegalArgumentException("Server tick must not be negative.");
        }
    }
}
