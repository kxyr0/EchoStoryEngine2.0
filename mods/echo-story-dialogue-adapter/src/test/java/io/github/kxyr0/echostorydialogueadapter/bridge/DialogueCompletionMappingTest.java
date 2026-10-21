package io.github.kxyr0.echostorydialogueadapter.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialogueCompletionMappingTest {
    @Test
    void completedDialogueMapsToSuccessCompletion() {
        List<ActionCompletion> completions = new ArrayList<>();

        DialogueCompletionMapping.complete(view(DialogueSessionStatus.COMPLETED), storyId(), executionId(),
                actionExecutionId(), completion -> {
                    completions.add(completion);
                    return ActionCompletionAcceptance.ACCEPTED;
                });

        ActionCompletion completion = completions.get(0);
        assertEquals(ExternalActionResultCategory.SUCCESS, completion.resultCategory());
        assertEquals("COMPLETED", completion.outputs().get("dialogue.status"));
        assertEquals("sample:accepted", completion.outputs().get("outcome"));
        assertEquals("sample:accepted", completion.outputs().get("dialogue.terminalResult"));
    }

    @Test
    void cancelledDialogueMapsToCancellationCompletion() {
        List<ActionCompletion> completions = new ArrayList<>();

        DialogueCompletionMapping.complete(view(DialogueSessionStatus.CANCELLED), storyId(), executionId(),
                actionExecutionId(), completion -> {
                    completions.add(completion);
                    return ActionCompletionAcceptance.ACCEPTED;
                });

        assertEquals(ExternalActionResultCategory.CANCELLATION, completions.get(0).resultCategory());
    }

    @Test
    void waitingDialogueMapsToFailureCompletion() {
        List<ActionCompletion> completions = new ArrayList<>();

        DialogueCompletionMapping.complete(view(DialogueSessionStatus.WAITING), storyId(), executionId(),
                actionExecutionId(), completion -> {
                    completions.add(completion);
                    return ActionCompletionAcceptance.ACCEPTED;
                });

        assertEquals(ExternalActionResultCategory.FAILURE, completions.get(0).resultCategory());
    }

    private DialogueSessionView view(DialogueSessionStatus status) {
        return new DialogueSessionView(new DialogueSessionId("session-1"), DialogueId.parse("sample:greeting"),
                new AudienceId("player-1"), status, 1, 1, terminalResult(status),
                status == DialogueSessionStatus.CANCELLED ? Optional.of("explicit-cancel") : Optional.empty(),
                Optional.empty());
    }

    private Optional<String> terminalResult(DialogueSessionStatus status) {
        return status == DialogueSessionStatus.COMPLETED ? Optional.of("sample:accepted") : Optional.empty();
    }

    private StoryId storyId() {
        return StoryId.of("sample:story");
    }

    private ExecutionId executionId() {
        return ExecutionId.of("sample:execution");
    }

    private ActionExecutionId actionExecutionId() {
        return ActionExecutionId.of("sample:action_1");
    }
}
