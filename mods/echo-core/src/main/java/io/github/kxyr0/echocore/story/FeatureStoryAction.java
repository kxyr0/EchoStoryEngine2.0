package io.github.kxyr0.echocore.story;

import io.github.kxyr0.echocore.api.FeatureFlags;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class FeatureStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    public FeatureStoryAction(MinecraftServer server) { this.server = server; }
    @Override public NamespacedId actionType() { return CoreStoryBridge.ACTION; }
    @Override public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(request.payload(), value -> sink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), ExternalActionResultCategory.SUCCESS, value)));
    }
    @Override public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(request.payload(), value -> sink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), ExternalActionResultCategory.SUCCESS, value)));
    }
    @Override public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.unsupported("Feature flags complete synchronously");
    }
    private ActionInvocationResult invoke(Map<String, String> raw, java.util.function.Consumer<Map<String, String>> completion) {
        try {
            FeatureStoryPayload payload = FeatureStoryPayload.parse(raw);
            List<UUID> players = resolve(payload.player());
            if (players.isEmpty()) return ActionInvocationResult.rejected("No players matched feature action");
            for (UUID player : players) FeatureFlags.set(player, payload.flag(), payload.value());
            completion.accept(Map.of("flag", payload.flag(), "value", Boolean.toString(payload.value())));
            return ActionInvocationResult.accepted();
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return ActionInvocationResult.rejected(failure.getMessage());
        }
    }
    private List<UUID> resolve(String player) {
        if (player.equals("@a")) return server.getPlayerList().getPlayers().stream().map(value -> value.getUUID()).toList();
        var named = server.getPlayerList().getPlayerByName(player);
        if (named != null) return List.of(named.getUUID());
        return List.of(UUID.fromString(player));
    }
}
