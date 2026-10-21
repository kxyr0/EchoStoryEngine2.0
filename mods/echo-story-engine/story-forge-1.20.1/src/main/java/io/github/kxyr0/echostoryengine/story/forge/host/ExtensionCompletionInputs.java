package io.github.kxyr0.echostoryengine.story.forge.host;

import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import java.util.Objects;

/**
 * Converts extension completion boundary data into neutral runtime input.
 */
final class ExtensionCompletionInputs {
    private ExtensionCompletionInputs() {
    }

    static StoryRuntimeInput from(ActionCompletion completion) {
        ActionCompletion checked = Objects.requireNonNull(completion, "completion");
        return StoryRuntimeInput.externalActionCompleted(checked.storyId(), checked.executionId(),
                new StoryRuntimeInput.ExternalActionCompletionInput(
                        checked.actionExecutionId(),
                        checked.resultCategory(),
                        checked.outputs()));
    }
}
