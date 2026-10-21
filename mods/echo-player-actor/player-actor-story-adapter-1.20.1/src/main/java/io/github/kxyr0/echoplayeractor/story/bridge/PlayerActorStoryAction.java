package io.github.kxyr0.echoplayeractor.story.bridge;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorService;
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
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

final class PlayerActorStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    private final NamespacedId actionType;

    PlayerActorStoryAction(MinecraftServer server, NamespacedId actionType) {
        this.server = server;
        this.actionType = actionType;
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), request.payload(), sink);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType(), request.payload(), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return actionType.equals(request.actionType())
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
        if (!actionType.equals(requestedType)) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + requestedType);
        }
        EchoPlayerActorServiceBridge.LookupResult lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() != EchoPlayerActorServiceBridge.Status.RESOLVED) {
            return ActionInvocationResult.rejected("Echo Player Actor service is unavailable.");
        }
        try {
            PlayerActorResult result = dispatch(lookup.service().orElseThrow(), actionId, payload);
            complete(storyId, executionId, actionId, result, sink);
            return ActionInvocationResult.accepted();
        } catch (IllegalArgumentException exception) {
            return ActionInvocationResult.rejected("Invalid echo_player_actor action payload.");
        }
    }

    private PlayerActorResult dispatch(
            PlayerActorService service,
            ActionExecutionId actionId,
            Map<String, String> payload
    ) {
        PlayerActorId actorId = PlayerActorStoryPayload.actorId(server, payload);
        if (PlayerActorStoryIds.SPAWN_AT.equals(actionType)) {
            return service.spawnAt(new PlayerActorSpawnRequest(PlayerActorStoryIds.commandId(actionId), actorId,
                    PlayerActorStoryIds.owner(actionId), position(payload), PlayerActorStoryPayload.number(payload,
                    "yaw"), PlayerActorStoryPayload.number(payload, "pitch")));
        }
        if (PlayerActorStoryIds.SET_HOTBAR_ITEM.equals(actionType)) {
            return service.setHotbarItem(new PlayerHotbarItemRequest(PlayerActorStoryIds.commandId(actionId), actorId,
                    PlayerActorStoryIds.owner(actionId), PlayerActorStoryPayload.integer(payload, "slot"),
                    PlayerActorStoryPayload.required(payload, "itemId"),
                    PlayerActorStoryPayload.integer(payload, "count", 1),
                    PlayerActorStoryPayload.bool(payload, "replace", false)));
        }
        if (PlayerActorStoryIds.SELECT_HOTBAR_SLOT.equals(actionType)) {
            return service.selectHotbarSlot(new PlayerHotbarSelectionRequest(PlayerActorStoryIds.commandId(actionId),
                    actorId, PlayerActorStoryIds.owner(actionId), PlayerActorStoryPayload.integer(payload, "slot")));
        }
        return service.sitAt(new PlayerSitRequest(PlayerActorStoryIds.commandId(actionId), actorId,
                PlayerActorStoryIds.owner(actionId), blockPosition(payload),
                PlayerActorStoryPayload.integer(payload, "timeoutTicks", 40)));
    }

    private Vector3 position(Map<String, String> payload) {
        return new Vector3(PlayerActorStoryPayload.number(payload, "x"),
                PlayerActorStoryPayload.number(payload, "y"),
                PlayerActorStoryPayload.number(payload, "z"));
    }

    private BlockPosition blockPosition(Map<String, String> payload) {
        return new BlockPosition(PlayerActorStoryPayload.integer(payload, "x"),
                PlayerActorStoryPayload.integer(payload, "y"),
                PlayerActorStoryPayload.integer(payload, "z"));
    }

    private void complete(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionId,
            PlayerActorResult result,
            ActionCompletionSink sink
    ) {
        Map<String, String> outputs = new HashMap<>();
        outputs.put("status", result.status().name());
        outputs.put("outcome", result.outcome().name());
        outputs.put("message", result.message());
        result.failure().ifPresent(failure -> outputs.put("failure", failure.name()));
        ExternalActionResultCategory category = terminalSuccess(result)
                ? ExternalActionResultCategory.SUCCESS
                : ExternalActionResultCategory.FAILURE;
        if (category == ExternalActionResultCategory.FAILURE) {
            broadcastFailure(actionId, result);
        }
        sink.complete(new ActionCompletion(storyId, executionId, actionId, actionType, category, outputs));
    }

    private void broadcastFailure(ActionExecutionId actionId, PlayerActorResult result) {
        String failure = result.failure().map(Enum::name).orElse("UNKNOWN");
        String text = "Echo Player Actor story action failed: " + actionType.asString()
                + " action=" + actionId.asString() + " failure=" + failure + " message=" + result.message();
        server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    private boolean terminalSuccess(PlayerActorResult result) {
        return result.status() == PlayerActorStatus.SUCCEEDED || result.status() == PlayerActorStatus.DUPLICATE;
    }
}
