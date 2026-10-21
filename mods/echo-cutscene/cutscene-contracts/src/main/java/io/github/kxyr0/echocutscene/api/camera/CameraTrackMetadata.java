package io.github.kxyr0.echocutscene.api.camera;

import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import java.util.Objects;

public record CameraTrackMetadata(
        CameraTrackId trackId,
        CameraTrackRevision revision,
        CameraTrackDataId dataId,
        long durationMillis,
        int sampleRate,
        int schemaVersion,
        String fingerprint
) {
    public CameraTrackMetadata {
        trackId = Objects.requireNonNull(trackId, "trackId");
        revision = Objects.requireNonNull(revision, "revision");
        dataId = Objects.requireNonNull(dataId, "dataId");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint").trim();
        if (durationMillis < 0L || sampleRate <= 0 || schemaVersion <= 0 || fingerprint.isEmpty()) {
            throw new IllegalArgumentException("Camera track metadata values are invalid.");
        }
    }
}
