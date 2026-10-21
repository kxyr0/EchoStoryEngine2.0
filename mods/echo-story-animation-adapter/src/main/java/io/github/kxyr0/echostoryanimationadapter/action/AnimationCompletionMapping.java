package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Converts Animation public API outcomes into stable Story action completions.
 */
final class AnimationCompletionMapping {
    private AnimationCompletionMapping() {
    }

    static void started(
            Scope scope,
            AnimationActionPayload payload,
            StartedPlayback started,
            ActionCompletionSink sink
    ) {
        Map<String, String> outputs = base(payload, started.commandId().value());
        outputs.put("outcome", "STARTED");
        outputs.put("phase", started.query().phase().map(Enum::name).orElseGet(() -> started.playResult().initialPhase()
                .map(Enum::name).orElse("")));
        outputs.put("animationPlaybackId", started.playResult().playbackId().map(Object::toString)
                .orElseGet(() -> started.query().playbackId().map(Object::toString).orElse("")));
        outputs.put("diagnosticCode", started.playResult().diagnosticCode());
        outputs.put("subjectRevision", Long.toString(started.query().subjectRevision()));
        outputs.put("playbackRevision", Long.toString(started.query().playbackRevision()));
        complete(scope, ExternalActionResultCategory.SUCCESS, outputs, sink);
    }

    static void terminal(
            Scope scope,
            AnimationActionPayload payload,
            AnimationPlaybackQueryResult query,
            ActionCompletionSink sink
    ) {
        Map<String, String> outputs = base(payload, "");
        outputs.put("outcome", query.outcome().map(Enum::name).orElse("FAILED"));
        outputs.put("phase", "TERMINAL");
        outputs.put("animationPlaybackId", query.playbackId().map(Object::toString).orElse(""));
        outputs.put("diagnosticCode", query.diagnosticCode());
        outputs.put("subjectRevision", Long.toString(query.subjectRevision()));
        outputs.put("playbackRevision", Long.toString(query.playbackRevision()));
        complete(scope, ExternalActionResultCategory.SUCCESS, outputs, sink);
    }

    static void failure(
            Scope scope,
            AnimationActionPayload payload,
            String code,
            String message,
            ActionCompletionSink sink
    ) {
        Map<String, String> outputs = base(payload, "");
        outputs.put("outcome", "FAILED");
        outputs.put("phase", "");
        outputs.put("animationPlaybackId", "");
        outputs.put("diagnosticCode", Objects.requireNonNull(code, "code"));
        outputs.put("message", Objects.requireNonNull(message, "message"));
        complete(scope, ExternalActionResultCategory.FAILURE, outputs, sink);
    }

    private static Map<String, String> base(AnimationActionPayload payload, String commandId) {
        Map<String, String> outputs = new LinkedHashMap<>();
        outputs.put("animationId", payload.animationId().toString());
        outputs.put("animationSubjectId", payload.subjectId().toString());
        outputs.put("commandId", commandId);
        return outputs;
    }

    private static void complete(
            Scope scope,
            ExternalActionResultCategory category,
            Map<String, String> outputs,
            ActionCompletionSink sink
    ) {
        sink.complete(new ActionCompletion(scope.storyId(), scope.executionId(), scope.actionExecutionId(),
                scope.actionType(), category, outputs));
    }

    record Scope(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            NamespacedId actionType
    ) {
        Scope {
            storyId = Objects.requireNonNull(storyId, "storyId");
            executionId = Objects.requireNonNull(executionId, "executionId");
            actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
            actionType = Objects.requireNonNull(actionType, "actionType");
        }
    }

    record StartedPlayback(
            AnimationPlayResult playResult,
            AnimationPlaybackQueryResult query,
            AnimationCommandId commandId
    ) {
        StartedPlayback {
            playResult = Objects.requireNonNull(playResult, "playResult");
            query = Objects.requireNonNull(query, "query");
            commandId = Objects.requireNonNull(commandId, "commandId");
        }
    }
}
