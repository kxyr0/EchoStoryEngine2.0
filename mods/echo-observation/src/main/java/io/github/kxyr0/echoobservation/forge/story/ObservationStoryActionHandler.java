package io.github.kxyr0.echoobservation.forge.story;

import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.LinkedHashMap;
import java.util.Map;

final class ObservationStoryActionHandler implements StoryActionHandler {
    private final ObservationService service;
    private final ObservationStoryOperation operation;

    ObservationStoryActionHandler(ObservationService service, ObservationStoryOperation operation) {
        this.service = service;
        this.operation = operation;
    }

    @Override public NamespacedId actionType() { return operation.actionType(); }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        if (!operation.actionType().equals(request.actionType())) {
            return ActionInvocationResult.unsupported("Unsupported Observation action");
        }
        var player = ObservationStoryPlayerContext.resolve(request.executionId().asString(), request.payload());
        String target = request.payload().get("target");
        if (player.isEmpty() || target == null || target.isBlank()) {
            return ActionInvocationResult.rejected("OBSERVATION_PLAYER_CONTEXT_MISSING");
        }
        var result = operation.execute(service, player.orElseThrow(), target);
        Map<String, String> outputs = new LinkedHashMap<>();
        result.value().ifPresent(value -> outputs.put(request.payload().getOrDefault("outputKey", "value"), value));
        if (!result.successful()) outputs.put("error", result.code());
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), result.successful() ? ExternalActionResultCategory.SUCCESS
                        : ExternalActionResultCategory.FAILURE, outputs));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return start(new ActionStartRequest(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), request.payload()), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.accepted();
    }
}
