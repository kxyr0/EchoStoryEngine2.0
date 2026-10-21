package io.github.kxyr0.echonpcnavigationadapter.story;

import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationScriptFacade;
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

final class NpcStopStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    private final NpcNavigationScriptFacade facade = new NpcNavigationScriptFacade();

    NpcStopStoryAction(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return NpcNavigationStoryExtensionFactory.STOP;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        facade.stop(server, NpcNavigationPayloads.actor(request.payload()));
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), ExternalActionResultCategory.SUCCESS, Map.of("stopped", "true")));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), ExternalActionResultCategory.SUCCESS, Map.of("stopped", "true")));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.accepted();
    }
}
