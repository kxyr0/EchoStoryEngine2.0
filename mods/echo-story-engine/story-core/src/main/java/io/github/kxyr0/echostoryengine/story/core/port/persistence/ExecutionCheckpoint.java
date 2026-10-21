package io.github.kxyr0.echostoryengine.story.core.port.persistence;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import java.util.Objects;
import java.util.Optional;

/**
 * Storage port payload owned by core, distinct from any persisted schema.
 */
public record ExecutionCheckpoint(
        StoryExecutionState state,
        CompiledStoryFingerprint definitionFingerprint,
        Optional<PendingActionCheckpoint> pendingAction) {
    public ExecutionCheckpoint {
        state = Objects.requireNonNull(state, "state");
        definitionFingerprint = Objects.requireNonNull(definitionFingerprint, "definitionFingerprint");
        pendingAction = Objects.requireNonNull(pendingAction, "pendingAction");
    }

    /**
     * Creates a checkpoint without a pending action.
     */
    public static ExecutionCheckpoint withoutPending(
            StoryExecutionState state,
            CompiledStoryFingerprint definitionFingerprint) {
        return new ExecutionCheckpoint(state, definitionFingerprint, Optional.empty());
    }
}
