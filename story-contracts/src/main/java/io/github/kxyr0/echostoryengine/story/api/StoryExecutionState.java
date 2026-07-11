package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;

/**
 * Serializable execution position for the minimal runtime state machine.
 *
 * <p>The state is data-only and can be inspected, stored, migrated, and replayed by future ports. It intentionally
 * does not hold callbacks, threads, lambdas, platform objects, or third-party mod objects.
 */
public record StoryExecutionState(StoryId storyId, ExecutionId executionId, NodeId currentNode, int revision) {
    public StoryExecutionState {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        currentNode = Objects.requireNonNull(currentNode, "currentNode");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    /**
     * Creates the first state for a known story execution.
     */
    public static StoryExecutionState start(StoryId storyId, ExecutionId executionId) {
        return new StoryExecutionState(storyId, executionId, NodeId.root(), 0);
    }

    /**
     * Returns the next immutable state revision.
     */
    public StoryExecutionState advanceTo(NodeId nextNode) {
        return new StoryExecutionState(storyId, executionId, nextNode, revision + 1);
    }
}
