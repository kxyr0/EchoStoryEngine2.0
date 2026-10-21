package io.github.kxyr0.echoanimationeditor.api.session;

import java.util.Objects;
import java.util.UUID;

public record AnimatorDraftId(String value) {
    public AnimatorDraftId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }

    public static AnimatorDraftId random() {
        return new AnimatorDraftId(UUID.randomUUID().toString());
    }
}
