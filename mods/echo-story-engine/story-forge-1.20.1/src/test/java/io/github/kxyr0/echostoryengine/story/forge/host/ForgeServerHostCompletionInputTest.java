package io.github.kxyr0.echostoryengine.story.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ForgeServerHostCompletionInputTest {
    private static final StoryId STORY_ID = StoryId.of("sample:story");
    private static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:execution");
    private static final ActionExecutionId ACTION_ID = ActionExecutionId.of("sample:execution/action");
    private static final NamespacedId ACTION_TYPE = NamespacedId.of("sample:external");

    @Test
    void extensionCompletionOutputsReachRuntimeInput() {
        ActionCompletion completion = new ActionCompletion(
                STORY_ID,
                EXECUTION_ID,
                ACTION_ID,
                ACTION_TYPE,
                ExternalActionResultCategory.SUCCESS,
                Map.of("external.result", "accepted"));

        var input = ExtensionCompletionInputs.from(completion);

        assertEquals(ACTION_ID, input.completedActionExecutionId());
        assertEquals(ExternalActionResultCategory.SUCCESS, input.completedActionResultCategory());
        assertEquals("accepted", input.completedActionOutputs().get("external.result"));
    }
}
