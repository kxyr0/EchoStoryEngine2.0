package io.github.kxyr0.echoscene.forge.audio;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import io.github.kxyr0.echoscene.forge.command.MovementCommandAdapter;
import io.github.kxyr0.echoscene.forge.command.SceneCommandHub;
import io.github.kxyr0.echoscene.api.command.*;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import java.util.*;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

public final class AudioCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final MovementCommandAdapter.LocationCatalog locations;
    private final Set<SceneExecutionId> owners = new HashSet<>();
    public AudioCommandAdapter(Function<SceneExecutionId, ServerPlayer> players, MovementCommandAdapter.LocationCatalog locations) {
        this.players = players; this.locations = locations;
    }
    public SceneCommandStart start(SceneCommandRequest request, SceneCommandHub.Completion completion) {
        ServerPlayer player = players.apply(request.executionId());
        if (player == null) return SceneCommandStart.failed("AUDIO_PLAYBACK_START_FAILED: player unavailable");
        AudioCue cue = AudioCue.fromArguments(request.command().arguments());
        var location = cue.location().isEmpty() ? null : locations.find(cue.location()).orElseThrow(() ->
                new IllegalArgumentException("AUDIO_LOCATION_NOT_FOUND: " + cue.location()));
        owners.add(request.executionId());
        AudioServer.of(player.getServer()).play(player, owner(request.executionId()), cue,
                location == null ? "" : location.dimension(), location == null ? 0 : location.x(),
                location == null ? 0 : location.y(), location == null ? 0 : location.z(), detail -> {
                    if (cue.waitForEnd()) completion.complete(request.executionId(), request.command().stepId(), detail.isEmpty(), detail);
                });
        return cue.waitForEnd() ? SceneCommandStart.waiting("audio:" + request.executionId().value()
                + "/" + request.command().stepId().value()) : SceneCommandStart.completed();
    }
    public void cancelOwned(SceneExecutionId execution) {
        if (!owners.remove(execution)) return;
        ServerPlayer player = players.apply(execution);
        if (player != null) AudioServer.of(player.getServer()).cancelOwned(owner(execution));
    }
    private String owner(SceneExecutionId execution) { return "scene:" + execution.value(); }
}
