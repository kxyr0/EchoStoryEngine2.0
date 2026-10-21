package io.github.kxyr0.echostoryengine.story.forge.extension.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ExtensionCompletionQueueTest {
    private static final StoryId STORY_ID = StoryId.of("sample:story");
    private static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:execution");
    private static final NamespacedId ACTION_TYPE = NamespacedId.of("sample:action");
    private static final ActionExecutionId ACTION_ID = ActionExecutionId.of("sample:execution_action");

    @Test
    void acceptsOneCompletionAndRejectsDuplicate() {
        ExtensionCompletionQueue queue = new ExtensionCompletionQueue(2);
        queue.track(state(), command());

        assertEquals(ActionCompletionAcceptance.ACCEPTED, queue.sink().complete(completion(ACTION_ID)));
        assertEquals(ActionCompletionAcceptance.DUPLICATE, queue.sink().complete(completion(ACTION_ID)));

        List<ActionCompletion> drained = new ArrayList<>();
        assertEquals(1, queue.drain(4, drained::add));
        assertEquals(ACTION_ID, drained.get(0).actionExecutionId());
    }

    @Test
    void unknownAndShutdownAreRejected() {
        ExtensionCompletionQueue queue = new ExtensionCompletionQueue(1);

        assertEquals(ActionCompletionAcceptance.UNKNOWN_ACTION, queue.sink().complete(completion(ACTION_ID)));
        queue.shutdown();
        assertEquals(ActionCompletionAcceptance.SHUTDOWN, queue.sink().complete(completion(ACTION_ID)));
    }

    private StoryExecutionState state() {
        return StoryExecutionState.start(STORY_ID, EXECUTION_ID).waitingAt(
                io.github.kxyr0.echostoryengine.story.api.identity.NodeId.of("action"),
                ACTION_ID);
    }

    private ExternalCommand command() {
        return new ExternalCommand(ACTION_TYPE, ACTION_ID, Map.of());
    }

    private ActionCompletion completion(ActionExecutionId actionExecutionId) {
        return new ActionCompletion(STORY_ID, EXECUTION_ID, actionExecutionId, ACTION_TYPE,
                ExternalActionResultCategory.SUCCESS, Map.of());
    }
}
