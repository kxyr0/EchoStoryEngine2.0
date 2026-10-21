package io.github.kxyr0.echostoryengine.story.forge.host.readmodel;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable command read model for the current server host.
 */
public record HostSnapshot(
        HostHealth health,
        List<ExecutionSummary> executions) {
    public HostSnapshot {
        health = Objects.requireNonNull(health, "health");
        executions = List.copyOf(Objects.requireNonNull(executions, "executions"));
    }

    public Optional<ExecutionSummary> findExecution(ExecutionId executionId) {
        return executions.stream()
                .filter(summary -> summary.executionId().equals(executionId))
                .findFirst();
    }
}
