package io.github.kxyr0.echotrigger.core.persistence;

import io.github.kxyr0.echotrigger.api.id.TriggerExecutionId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import java.util.Objects;

public record TriggerInstanceState(
        TriggerId triggerId,
        TriggerInstanceId instanceId,
        TriggerExecutionId executionId,
        TriggerOwnerId owner,
        TriggerRevision revision,
        int step,
        TriggerStatus status,
        TriggerOutcome outcome
) {
    public TriggerInstanceState {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        owner = Objects.requireNonNull(owner, "owner");
        revision = Objects.requireNonNull(revision, "revision");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (step < 0) {
            throw new IllegalArgumentException("step must not be negative.");
        }
    }
}
