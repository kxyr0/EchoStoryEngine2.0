package io.github.kxyr0.echoaudio.forge.story;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.*;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.*;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.*;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class AudioStoryHandler implements StoryActionHandler {
    private final MinecraftServer server;
    private final Set<ActionExecutionId> pending = new HashSet<>();
    private final Map<ActionExecutionId, String> completed = new LinkedHashMap<>();
    AudioStoryHandler(MinecraftServer server) { this.server = server; }
    @Override public NamespacedId actionType() { return AudioStoryFactory.ACTION; }
    @Override public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        if (completed.containsKey(request.actionExecutionId())) {
            complete(request, sink, completed.get(request.actionExecutionId())); return ActionInvocationResult.accepted();
        }
        if (pending.contains(request.actionExecutionId())) return ActionInvocationResult.accepted();
        if (pending.size() >= 1024) return ActionInvocationResult.rejected("AUDIO_PLAYBACK_START_FAILED: action capacity");
        try {
            AudioCue cue = AudioCue.decode(request.payload());
            ServerPlayer player = player(request.executionId());
            AudioLocationLookupEvent.Position position = null;
            if (!cue.location().isEmpty()) {
                var lookup = new AudioLocationLookupEvent(cue.location());
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(lookup); position = lookup.require();
            }
            String owner = "story:" + request.executionId().asString(); pending.add(request.actionExecutionId());
            AudioServer.of(server).play(player, owner, cue, position == null ? "" : position.dimension(),
                    position == null ? 0 : position.x(), position == null ? 0 : position.y(), position == null ? 0 : position.z(), detail -> {
                if (cue.waitForEnd()) complete(request, sink, detail);
            });
            if (!cue.waitForEnd()) complete(request, sink, "");
            return ActionInvocationResult.accepted();
        } catch (RuntimeException failure) {
            pending.remove(request.actionExecutionId()); return ActionInvocationResult.rejected(failure.getMessage());
        }
    }
    @Override public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        // Incomplete presentation is interrupted rather than replayed indefinitely after a crash.
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                actionType(), ExternalActionResultCategory.FAILURE, Map.of("audio.reason", "AUDIO_PLAYBACK_INTERRUPTED")));
        return ActionInvocationResult.accepted();
    }
    @Override public ActionInvocationResult cancel(ActionCancelRequest request) {
        AudioServer.of(server).cancelOwned("story:" + request.executionId().asString());
        return ActionInvocationResult.accepted();
    }
    void close() { AudioServer.of(server).cancelOwners("story:"); pending.clear(); completed.clear(); }
    private ServerPlayer player(ExecutionId execution) {
        String value = execution.asString(); int index = value.indexOf("/player/");
        if (index >= 0) {
            String raw = value.substring(index + 8).split("/", 2)[0];
            var player = server.getPlayerList().getPlayer(UUID.fromString(raw));
            if (player != null) return player;
        }
        var players = server.getPlayerList().getPlayers();
        if (players.size() == 1) return players.get(0);
        throw new IllegalArgumentException("AUDIO_PLAYBACK_START_FAILED: current player unresolved");
    }
    private void complete(ActionStartRequest request, ActionCompletionSink sink, String detail) {
        pending.remove(request.actionExecutionId());
        if (completed.size() >= 1024 && !completed.containsKey(request.actionExecutionId())) {
            completed.remove(completed.keySet().iterator().next());
        }
        completed.put(request.actionExecutionId(), detail);
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(), request.actionExecutionId(),
                actionType(), detail.isEmpty() ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE,
                Map.of("audio.reason", detail)));
    }
}
