package io.github.kxyr0.echoanimationauthoring.api.draft;

import java.util.Objects;
import java.util.UUID;

public record AuthoringDraftId(String value) {
    public AuthoringDraftId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }

    public static AuthoringDraftId random() {
        return new AuthoringDraftId(UUID.randomUUID().toString());
    }
}
