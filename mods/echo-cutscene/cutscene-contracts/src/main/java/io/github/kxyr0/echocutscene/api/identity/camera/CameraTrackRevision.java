package io.github.kxyr0.echocutscene.api.identity.camera;

public record CameraTrackRevision(long value) {
    public CameraTrackRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("CameraTrackRevision must not be negative.");
        }
    }

    public CameraTrackRevision next() {
        return new CameraTrackRevision(value + 1L);
    }
}
