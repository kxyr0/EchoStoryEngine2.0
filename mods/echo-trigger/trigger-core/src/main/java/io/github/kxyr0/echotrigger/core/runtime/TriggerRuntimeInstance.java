package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.id.TriggerExecutionId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import java.util.Objects;

public record TriggerRuntimeInstance(
        CompiledTriggerDefinition definition,
        TriggerInstanceId instanceId,
        TriggerExecutionId executionId,
        TriggerOwnerId owner,
        int step,
        TriggerStatus status,
        TriggerOutcome outcome,
        long generation
) {
    public TriggerRuntimeInstance {
        definition = Objects.requireNonNull(definition, "definition");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        owner = Objects.requireNonNull(owner, "owner");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (step < 0 || generation < 0) {
            throw new IllegalArgumentException("step and generation must not be negative.");
        }
    }

    public TriggerSnapshot snapshot() {
        return new TriggerSnapshot(definition.id(), instanceId, executionId, status, outcome,
                step, owner, definition.revision());
    }

    public TriggerRuntimeInstance stopped() {
        return with(step, TriggerStatus.STOPPED, TriggerOutcome.STOPPED);
    }

    public TriggerRuntimeInstance completed() {
        return with(definition.sequence().size(), TriggerStatus.COMPLETED, TriggerOutcome.COMPLETED);
    }

    public TriggerRuntimeInstance with(int nextStep, TriggerStatus nextStatus, TriggerOutcome nextOutcome) {
        return new TriggerRuntimeInstance(definition, instanceId, executionId, owner, nextStep,
                nextStatus, nextOutcome, generation);
    }
}
