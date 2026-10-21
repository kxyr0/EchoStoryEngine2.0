package io.github.kxyr0.echostoryengine.story.persistence.schema;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.PendingActionCheckpoint;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts between core checkpoint contracts and save schema V1 snapshots.
 */
public final class ExecutionSnapshotTranslator {
    /**
     * Creates a schema snapshot from a core checkpoint.
     */
    public ExecutionSnapshot fromCheckpoint(
            ExecutionCheckpoint checkpoint,
            Instant createdAt,
            Instant updatedAt) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        StoryExecutionState state = checkpoint.state();
        return new ExecutionSnapshot(
                ExecutionSnapshot.SUPPORTED_SCHEMA_VERSION,
                state.storyId().asString(),
                state.executionId().asString(),
                checkpoint.definitionFingerprint().value(),
                ExecutionSnapshot.DEFINITION_REVISION,
                state.revision(),
                state.phase().name(),
                state.currentNode().value(),
                state.variables(),
                state.pendingActionExecutionId(),
                checkpoint.pendingAction().map(this::pendingSnapshot),
                state.lastCompletedActionExecutionId(),
                state.failureCode(),
                createdAt.toString(),
                updatedAt.toString());
    }

    /**
     * Creates a core checkpoint from a schema snapshot.
     */
    public ExecutionCheckpoint toCheckpoint(ExecutionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        StoryExecutionState state = state(snapshot);
        return new ExecutionCheckpoint(
                state,
                new CompiledStoryFingerprint(snapshot.definitionFingerprint()),
                snapshot.pendingAction().map(this::pendingCheckpoint));
    }

    private StoryExecutionState state(ExecutionSnapshot snapshot) {
        return new StoryExecutionState(
                StoryId.of(snapshot.storyId()),
                ExecutionId.of(snapshot.executionId()),
                NodeId.of(snapshot.currentNode()),
                StoryExecutionPhase.valueOf(snapshot.phase()),
                snapshot.variables(),
                snapshot.pendingActionExecutionId(),
                snapshot.lastCompletedActionExecutionId(),
                snapshot.failureCode(),
                snapshot.executionRevision());
    }

    private PendingActionSnapshot pendingSnapshot(PendingActionCheckpoint checkpoint) {
        return new PendingActionSnapshot(
                checkpoint.actionExecutionId().asString(),
                checkpoint.actionType().asString(),
                checkpoint.arguments());
    }

    private PendingActionCheckpoint pendingCheckpoint(PendingActionSnapshot snapshot) {
        return new PendingActionCheckpoint(
                ActionExecutionId.of(snapshot.actionExecutionId()),
                NamespacedId.of(snapshot.actionType()),
                snapshot.arguments());
    }
}
