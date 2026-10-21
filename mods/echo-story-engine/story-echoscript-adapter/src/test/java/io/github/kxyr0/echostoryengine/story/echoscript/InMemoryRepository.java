package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionStateRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InMemoryRepository implements ExecutionStateRepository {
    private final Map<ExecutionId, ExecutionCheckpoint> checkpoints = new HashMap<>();

    @Override
    public ExecutionLoadResult load(ExecutionId executionId) {
        ExecutionCheckpoint checkpoint = checkpoints.get(executionId);
        return checkpoint == null ? ExecutionLoadResult.notFound(List.of()) : ExecutionLoadResult.found(checkpoint,
                List.of());
    }

    @Override
    public ExecutionSaveResult save(ExecutionCheckpoint checkpoint, int expectedRevision) {
        ExecutionCheckpoint current = checkpoints.get(checkpoint.state().executionId());
        if (!matchesRevision(current, expectedRevision)) {
            return ExecutionSaveResult.revisionConflict(List.of());
        }
        checkpoints.put(checkpoint.state().executionId(), checkpoint);
        return ExecutionSaveResult.saved(List.of());
    }

    private boolean matchesRevision(ExecutionCheckpoint current, int expectedRevision) {
        if (expectedRevision == -1) {
            return current == null;
        }
        return current != null && current.state().revision() == expectedRevision;
    }
}
