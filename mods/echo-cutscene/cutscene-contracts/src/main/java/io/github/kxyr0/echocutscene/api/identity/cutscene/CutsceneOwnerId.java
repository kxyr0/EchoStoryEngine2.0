package io.github.kxyr0.echocutscene.api.identity.cutscene;

import java.util.Objects;
import java.util.UUID;

public record CutsceneOwnerId(String value) {
    public CutsceneOwnerId {
        value = require(value, "CutsceneOwnerId");
    }

    public static CutsceneOwnerId player(UUID uuid) {
        return new CutsceneOwnerId("player:" + Objects.requireNonNull(uuid, "uuid"));
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
