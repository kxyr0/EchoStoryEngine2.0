package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps public Echo Dialogue terminal views to Story action completions.
 */
final class DialogueCompletionMapping {
    private DialogueCompletionMapping() {
    }

    static void complete(
            DialogueSessionView session,
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            ActionCompletionSink completionSink
    ) {
        completionSink.complete(new ActionCompletion(storyId, executionId, actionExecutionId,
                DialogueBridgeIds.ACTION_TYPE, resultCategory(session.status()), outputs(session)));
    }

    static void completeFailure(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            String reason,
            ActionCompletionSink completionSink
    ) {
        completionSink.complete(new ActionCompletion(storyId, executionId, actionExecutionId,
                DialogueBridgeIds.ACTION_TYPE, ExternalActionResultCategory.FAILURE,
                Map.of("dialogue.status", "FAILED", "dialogue.failureReason", reason)));
    }

    private static Map<String, String> outputs(DialogueSessionView session) {
        Map<String, String> values = new HashMap<>();
        values.put("dialogue.sessionId", session.sessionId().toString());
        values.put("dialogue.status", session.status().name());
        session.terminalOutcomeId().ifPresent(value -> values.put("outcome", value.toString()));
        session.terminalResult().ifPresent(value -> values.put("dialogue.terminalResult", value));
        session.cancellationReason().ifPresent(value -> values.put("dialogue.cancellationReason", value));
        session.blockedReason().ifPresent(value -> values.put("dialogue.blockedReason", value));
        return values;
    }

    private static ExternalActionResultCategory resultCategory(DialogueSessionStatus status) {
        return switch (status) {
            case COMPLETED -> ExternalActionResultCategory.SUCCESS;
            case CANCELLED -> ExternalActionResultCategory.CANCELLATION;
            case ACTIVE, WAITING, FAILED, BLOCKED -> ExternalActionResultCategory.FAILURE;
        };
    }
}
