package io.github.kxyr0.echostoryengine.blackspace.story;

import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance;
import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceScene;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class CreateBlackspaceStoryAction implements StoryActionHandler {
    private final MinecraftServer server;

    CreateBlackspaceStoryAction(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return BlackspaceStoryIds.CREATE;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(request.storyId(), request.executionId(), request.actionExecutionId(), request.actionType(),
                request.payload(), sink);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(request.storyId(), request.executionId(), request.actionExecutionId(), request.actionType(),
                request.payload(), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return BlackspaceStoryIds.CREATE.equals(request.actionType())
                ? ActionInvocationResult.accepted()
                : ActionInvocationResult.unsupported("Unsupported action type: " + request.actionType());
    }

    private ActionInvocationResult invoke(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionId,
            NamespacedId requestedType,
            Map<String, String> payload,
            ActionCompletionSink sink
    ) {
        if (!BlackspaceStoryIds.CREATE.equals(requestedType)) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + requestedType);
        }
        try {
            ServerPlayer player = BlackspaceStoryPayload.player(server, payload);
            BlackspaceInstance instance = BlackspaceScene.create(player, BlackspaceStoryPayload.instanceId(payload));
            Map<String, String> outputs = new LinkedHashMap<>(instance.outputs());
            BlackspaceStoryPayload.optional(payload, "spawnOutput")
                    .ifPresent(outputName -> outputs.put(outputName, instance.spawnValue()));
            sink.complete(new ActionCompletion(storyId, executionId, actionId, requestedType,
                    ExternalActionResultCategory.SUCCESS, outputs));
            return ActionInvocationResult.accepted();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ActionInvocationResult.rejected("Invalid echo_blackspace:create payload: "
                    + exception.getMessage());
        }
    }
}
