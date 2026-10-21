package io.github.kxyr0.echocutscene.forge.api.service;

import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import java.util.Objects;
import java.util.UUID;

public record CutscenePlaybackView(
        CutscenePlaybackId playbackId,
        CutsceneId cutsceneId,
        CameraTrackId cameraTrackId,
        UUID playerId,
        CutsceneStatus status,
        long elapsedMillis,
        double speed
) {
    public CutscenePlaybackView {
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        cutsceneId = Objects.requireNonNull(cutsceneId, "cutsceneId");
        cameraTrackId = Objects.requireNonNull(cameraTrackId, "cameraTrackId");
        playerId = Objects.requireNonNull(playerId, "playerId");
        status = Objects.requireNonNull(status, "status");
    }

    public boolean prepared() {
        return status == CutsceneStatus.PLAYING || status == CutsceneStatus.HOLDING_LAST_FRAME;
    }
}
