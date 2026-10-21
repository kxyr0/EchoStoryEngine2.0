package io.github.kxyr0.echostoryengine.story.forge.extension.dispatch;

import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;

/**
 * Engine-owned identity facts for one active external action.
 */
record TrackedAction(
        StoryId storyId,
        ExecutionId executionId,
        ActionExecutionId actionExecutionId,
        NamespacedId actionType) {
    TrackedAction {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
    }

    boolean matches(
            StoryId candidateStory,
            ExecutionId candidateExecution,
            NamespacedId candidateActionType) {
        return storyId.equals(candidateStory)
                && executionId.equals(candidateExecution)
                && actionType.equals(candidateActionType);
    }
}
