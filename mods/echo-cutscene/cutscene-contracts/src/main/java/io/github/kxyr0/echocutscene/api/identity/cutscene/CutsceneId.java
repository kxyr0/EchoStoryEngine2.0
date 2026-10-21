package io.github.kxyr0.echocutscene.api.identity.cutscene;

import java.util.Objects;

public record CutsceneId(String value) {
    public CutsceneId {
        value = token(value, "CutsceneId");
    }

    public static CutsceneId parse(String value) {
        return new CutsceneId(value);
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
