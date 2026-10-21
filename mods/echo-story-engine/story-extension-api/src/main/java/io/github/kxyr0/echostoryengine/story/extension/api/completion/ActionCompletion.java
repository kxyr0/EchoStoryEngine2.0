package io.github.kxyr0.echostoryengine.story.extension.api.completion;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable completion produced by an external action handler.
 */
public record ActionCompletion(
        StoryId storyId,
        ExecutionId executionId,
        ActionExecutionId actionExecutionId,
        NamespacedId actionType,
        ExternalActionResultCategory resultCategory,
        Map<String, String> outputs) {
    public ActionCompletion {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
        resultCategory = Objects.requireNonNull(resultCategory, "resultCategory");
        outputs = Map.copyOf(Objects.requireNonNull(outputs, "outputs"));
    }
}
