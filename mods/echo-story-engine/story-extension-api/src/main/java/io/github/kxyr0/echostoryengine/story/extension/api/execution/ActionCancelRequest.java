package io.github.kxyr0.echostoryengine.story.extension.api.execution;

import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;

/**
 * Server-thread request to cancel one external action.
 */
public record ActionCancelRequest(
        StoryId storyId,
        ExecutionId executionId,
        ActionExecutionId actionExecutionId,
        NamespacedId actionType) {
    public ActionCancelRequest {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
    }
}
