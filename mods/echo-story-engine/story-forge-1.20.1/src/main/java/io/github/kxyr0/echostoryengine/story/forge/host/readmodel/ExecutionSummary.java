package io.github.kxyr0.echostoryengine.story.forge.host.readmodel;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;

/**
 * Bounded command read model for one execution.
 */
public record ExecutionSummary(
        ExecutionId executionId,
        StoryId storyId,
        StoryExecutionPhase phase,
        String currentNode,
        String pendingActionType) {
    public ExecutionSummary {
        executionId = Objects.requireNonNull(executionId, "executionId");
        storyId = Objects.requireNonNull(storyId, "storyId");
        phase = Objects.requireNonNull(phase, "phase");
        currentNode = Objects.requireNonNull(currentNode, "currentNode");
        pendingActionType = Objects.requireNonNull(pendingActionType, "pendingActionType");
    }

    public static ExecutionSummary from(StoryExecutionState state, String pendingActionType) {
        return new ExecutionSummary(
                state.executionId(),
                state.storyId(),
                state.phase(),
                state.currentNode().value(),
                pendingActionType);
    }
}
