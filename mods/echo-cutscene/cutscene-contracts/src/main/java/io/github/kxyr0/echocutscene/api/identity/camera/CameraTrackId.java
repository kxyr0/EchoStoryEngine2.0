package io.github.kxyr0.echocutscene.api.identity.camera;

import java.util.Objects;

public record CameraTrackId(String value) {
    public CameraTrackId {
        value = token(value, "CameraTrackId");
    }

    public static CameraTrackId parse(String value) {
        return new CameraTrackId(value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String token(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (!trimmed.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespaced.");
        }
        return trimmed;
    }
}
