package io.github.kxyr0.echostoryengine.story.extension.api.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ActionCompletionTest {
    @Test
    void completionDefensivelyCopiesOutputs() {
        Map<String, String> outputs = new HashMap<>();
        outputs.put("result", "ok");

        ActionCompletion completion = new ActionCompletion(
                StoryId.of("sample:story"),
                ExecutionId.of("sample:execution"),
                ActionExecutionId.of("sample:execution_action"),
                NamespacedId.of("sample:action"),
                ExternalActionResultCategory.SUCCESS,
                outputs);
        outputs.clear();

        assertEquals("ok", completion.outputs().get("result"));
        assertThrows(UnsupportedOperationException.class, () -> completion.outputs().clear());
    }
}
