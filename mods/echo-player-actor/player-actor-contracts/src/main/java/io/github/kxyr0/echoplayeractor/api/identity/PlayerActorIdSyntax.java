package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.Objects;

public final class PlayerActorIdSyntax {
    private PlayerActorIdSyntax() {
    }

    public static String requireToken(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        if (trimmed.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(name + " must not contain spaces.");
        }
        return trimmed;
    }
}
