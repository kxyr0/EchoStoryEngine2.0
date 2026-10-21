package io.github.kxyr0.echocutscene.api.identity.cutscene;

import java.util.Objects;
import java.util.UUID;

public record CutscenePlaybackId(String value) {
    public CutscenePlaybackId {
        value = require(value, "CutscenePlaybackId");
    }

    public static CutscenePlaybackId create() {
        return new CutscenePlaybackId("playback-" + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }

    private static String require(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
