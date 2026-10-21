package io.github.kxyr0.echostoryengine.story.forge.infrastructure;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionStateRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-lifetime state repository used until a real persistence adapter is designed.
 *
 * <p>This adapter proves the port boundary without claiming durable save/load behavior.
 */
public final class VolatileExecutionStateRepository implements ExecutionStateRepository {
    private final Map<ExecutionId, ExecutionCheckpoint> checkpoints = new HashMap<>();

    @Override
    public synchronized ExecutionLoadResult load(ExecutionId executionId) {
        ExecutionCheckpoint checkpoint = checkpoints.get(Objects.requireNonNull(executionId, "executionId"));
        if (checkpoint == null) {
            return ExecutionLoadResult.notFound(List.of());
        }
        return ExecutionLoadResult.found(checkpoint, List.of());
    }

    @Override
    public synchronized ExecutionSaveResult save(ExecutionCheckpoint checkpoint, int expectedRevision) {
        ExecutionCheckpoint checked = Objects.requireNonNull(checkpoint, "checkpoint");
        ExecutionCheckpoint current = checkpoints.get(checked.state().executionId());
        if (!matchesRevision(current, expectedRevision)) {
            return ExecutionSaveResult.revisionConflict(List.of());
        }
        checkpoints.put(checked.state().executionId(), checked);
        return ExecutionSaveResult.saved(List.of());
    }

    @Override
    public synchronized List<ExecutionId> listExecutionIds() {
        return List.copyOf(checkpoints.keySet());
    }

    private boolean matchesRevision(ExecutionCheckpoint current, int expectedRevision) {
        if (expectedRevision == -1) {
            return current == null;
        }
        return current != null && current.state().revision() == expectedRevision;
    }
}
