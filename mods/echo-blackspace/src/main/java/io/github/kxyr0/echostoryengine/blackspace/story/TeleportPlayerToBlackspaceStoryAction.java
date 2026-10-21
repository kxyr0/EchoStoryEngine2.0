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

final class TeleportPlayerToBlackspaceStoryAction implements StoryActionHandler {
    private final MinecraftServer server;

    TeleportPlayerToBlackspaceStoryAction(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return BlackspaceStoryIds.TELEPORT_PLAYER;
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
        return BlackspaceStoryIds.TELEPORT_PLAYER.equals(request.actionType())
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
        if (!BlackspaceStoryIds.TELEPORT_PLAYER.equals(requestedType)) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + requestedType);
        }
        try {
            ServerPlayer player = BlackspaceStoryPayload.player(server, payload);
            String instanceId = BlackspaceStoryPayload.instanceId(payload);
            BlackspaceInstance instance = BlackspaceScene.find(player, instanceId)
                    .orElseThrow(() -> new IllegalArgumentException("Blackspace is not ready: " + instanceId));
            BlackspaceScene.enter(player, instance, () -> {
                Map<String, String> outputs = new LinkedHashMap<>();
                outputs.put("status", "TELEPORTED");
                outputs.putAll(instance.outputs());
                sink.complete(new ActionCompletion(storyId, executionId, actionId, requestedType,
                        ExternalActionResultCategory.SUCCESS, outputs));
            });
            return ActionInvocationResult.accepted();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ActionInvocationResult.rejected("Invalid echo_blackspace:teleport_player payload: "
                    + exception.getMessage());
        }
    }
}
