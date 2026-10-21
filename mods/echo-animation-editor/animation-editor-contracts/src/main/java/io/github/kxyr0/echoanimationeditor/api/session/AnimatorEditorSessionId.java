package io.github.kxyr0.echoanimationeditor.api.session;

import java.util.Objects;
import java.util.UUID;

public record AnimatorEditorSessionId(String value) {
    public AnimatorEditorSessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }

    public static AnimatorEditorSessionId random() {
        return new AnimatorEditorSessionId(UUID.randomUUID().toString());
    }
}
