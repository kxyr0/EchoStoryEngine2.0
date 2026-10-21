package io.github.kxyr0.echocutscene.api.identity.camera;

import java.util.Objects;

public record CameraTrackDataId(String value) {
    public CameraTrackDataId {
        value = Objects.requireNonNull(value, "CameraTrackDataId").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("CameraTrackDataId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
