package io.github.kxyr0.echovoice.api.playback;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import java.util.Objects;
import java.util.Optional;

/** Read-only status of the one foreground lane. */
public record VoicePlaybackSnapshot(
        VoicePlaybackStatus status,
        Optional<VoicePlaybackId> playbackId,
        Optional<VoicePlaybackRequest> request,
        Optional<VoiceAssetSnapshot> asset,
        long playedMillis,
        long soundEngineGeneration,
        Optional<VoiceStopReason> stopReason,
        int activeSourceCount,
        long staleStartRejects
) {
    public VoicePlaybackSnapshot {
        status = Objects.requireNonNull(status, "status");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        request = Objects.requireNonNull(request, "request");
        asset = Objects.requireNonNull(asset, "asset");
        stopReason = Objects.requireNonNull(stopReason, "stopReason");
    }

    public static VoicePlaybackSnapshot idle(long generation, long staleRejects) {
        return new VoicePlaybackSnapshot(VoicePlaybackStatus.IDLE, Optional.empty(), Optional.empty(),
                Optional.empty(), 0, generation, Optional.empty(), 0, staleRejects);
    }
}
