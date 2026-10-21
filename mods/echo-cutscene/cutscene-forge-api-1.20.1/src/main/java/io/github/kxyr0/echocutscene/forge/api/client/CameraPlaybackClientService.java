package io.github.kxyr0.echocutscene.forge.api.client;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;

public interface CameraPlaybackClientService {
    void begin(CutscenePlaybackId playbackId, CameraTrackSnapshot track, long startedAtMillis, double speed);

    default void begin(
            CutscenePlaybackId playbackId,
            CameraTrackSnapshot track,
            long startedAtMillis,
            double speed,
            CutsceneEndBehavior endBehavior
    ) {
        begin(playbackId, track, startedAtMillis, speed);
    }

    void end(CutscenePlaybackId playbackId);
}
