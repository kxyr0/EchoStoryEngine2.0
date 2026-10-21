package io.github.kxyr0.echotime.script.story;

import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.script.action.TimeActionExecutor;
import io.github.kxyr0.echotime.script.action.TimeActionResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import net.minecraft.server.MinecraftServer;

final class TimeStoryActionHandler implements StoryActionHandler {
    private final NamespacedId type;
    private final MinecraftServer server;
    private final TimeStoryWaits waits;

    TimeStoryActionHandler(NamespacedId type, MinecraftServer server, TimeStoryWaits waits) {
        this.type = type;
        this.server = server;
        this.waits = waits;
    }

    @Override public NamespacedId actionType() { return type; }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        var services = EchoTimeServiceAccess.find(server);
        if (services.isEmpty()) return ActionInvocationResult.rejected("Echo Time service unavailable.");
        TimeActionResult result = new TimeActionExecutor(services.orElseThrow()).execute(
                type.asString(), request.payload(), TimeStoryRequests.context(request));
        if (result.status() == TimeActionResult.Status.WAITING) {
            waits.register(request, sink, result.waitId().orElseThrow());
        } else sink.complete(completion(request, result));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        var services = EchoTimeServiceAccess.find(server);
        if (services.isEmpty()) return ActionInvocationResult.rejected("Echo Time service unavailable.");
        TimeActionResult result = new TimeActionExecutor(services.orElseThrow()).execute(
                type.asString(), request.payload(), TimeStoryRequests.context(request));
        if (result.status() == TimeActionResult.Status.WAITING) {
            waits.register(request, sink, result.waitId().orElseThrow());
        } else sink.complete(completion(request, result));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        waits.cancel(request.actionExecutionId().asString());
        EchoTimeServiceAccess.find(server).ifPresent(services -> services.mutations().resume(
                new io.github.kxyr0.echotime.api.identity.TimePauseOwner("story:" + request.executionId()),
                new io.github.kxyr0.echotime.api.identity.TimeMutationId("story-cancel:"
                        + request.actionExecutionId().asString())));
        return ActionInvocationResult.accepted();
    }

    private ActionCompletion completion(ActionStartRequest request, TimeActionResult result) {
        return new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(), type,
                category(result), result.outputs());
    }

    private ActionCompletion completion(ActionRecoveryRequest request, TimeActionResult result) {
        return new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(), type,
                category(result), result.outputs());
    }

    private ExternalActionResultCategory category(TimeActionResult result) {
        return result.status() == TimeActionResult.Status.SUCCESS
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
    }
}
