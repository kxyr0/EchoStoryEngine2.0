package io.github.kxyr0.echonpcinteraction.script.story;

import io.github.kxyr0.echonpcinteraction.script.NpcInteractionScriptFacade;
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
import net.minecraft.server.MinecraftServer;

final class NpcStandUpStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    private final NpcInteractionScriptFacade facade = new NpcInteractionScriptFacade();

    NpcStandUpStoryAction(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return NpcInteractionStoryExtensionFactory.STAND_UP;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        String owner = request.executionId().asString() + ":" + request.actionExecutionId().asString();
        facade.standUp(server, owner, request.payload());
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), ExternalActionResultCategory.SUCCESS, Map.of("standing", "true")));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return start(new ActionStartRequest(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), request.payload()), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.accepted();
    }
}
