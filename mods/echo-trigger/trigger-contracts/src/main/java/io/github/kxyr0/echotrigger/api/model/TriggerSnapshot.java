package io.github.kxyr0.echotrigger.api.model;

import io.github.kxyr0.echotrigger.api.id.TriggerExecutionId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import java.util.Objects;

public record TriggerSnapshot(
        TriggerId triggerId,
        TriggerInstanceId instanceId,
        TriggerExecutionId executionId,
        TriggerStatus status,
        TriggerOutcome outcome,
        int sequenceStep,
        TriggerOwnerId owner,
        TriggerRevision revision
) {
    public TriggerSnapshot {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        owner = Objects.requireNonNull(owner, "owner");
        revision = Objects.requireNonNull(revision, "revision");
        if (sequenceStep < 0) {
            throw new IllegalArgumentException("sequenceStep must not be negative.");
        }
    }
}
