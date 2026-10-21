package io.github.kxyr0.echoanimationauthoring.api.service;

import java.util.Objects;

public record AuthoringDraftOpenRequest(String clipId, String rigId) {
    public AuthoringDraftOpenRequest {
        clipId = requireText(clipId, "clipId");
        rigId = requireText(rigId, "rigId");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
