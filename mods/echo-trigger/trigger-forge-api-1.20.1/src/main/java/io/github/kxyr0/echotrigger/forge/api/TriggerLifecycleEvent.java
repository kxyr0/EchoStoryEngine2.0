package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import java.util.Objects;

public record TriggerLifecycleEvent(TriggerSnapshot snapshot, TriggerOutcome outcome) {
    public TriggerLifecycleEvent {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        outcome = Objects.requireNonNull(outcome, "outcome");
    }
}
