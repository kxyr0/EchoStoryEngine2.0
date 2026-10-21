package io.github.kxyr0.echocutscene.api.runtime;

import java.util.Objects;
import java.util.Optional;

public record CutsceneDiagnostic(String code, String message, Optional<String> source) {
    public CutsceneDiagnostic {
        code = Objects.requireNonNull(code, "code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        source = Objects.requireNonNull(source, "source");
        if (code.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("CutsceneDiagnostic code and message must not be blank.");
        }
    }

    public static CutsceneDiagnostic of(String code, String message) {
        return new CutsceneDiagnostic(code, message, Optional.empty());
    }
}
