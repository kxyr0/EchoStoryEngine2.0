package io.github.kxyr0.echostoryengine.testextension;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Objects;

final class TestStoryActionHandler implements StoryActionHandler {
    private final NamespacedId actionType;
    private final boolean success;

    TestStoryActionHandler(NamespacedId actionType, boolean success) {
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.success = success;
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink) {
        Thread worker = new Thread(() -> completionSink.complete(completion(request)), "echo-story-test-action");
        worker.setDaemon(true);
        worker.start();
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink) {
        completionSink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), category(), Map.of("recovered", "true")));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.accepted();
    }

    private ActionCompletion completion(ActionStartRequest request) {
        return new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), category(), Map.of("started", "true"));
    }

    private ExternalActionResultCategory category() {
        return success ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
    }
}
