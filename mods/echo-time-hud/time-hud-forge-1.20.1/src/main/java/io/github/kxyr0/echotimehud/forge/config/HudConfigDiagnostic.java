package io.github.kxyr0.echotimehud.forge.config;

import java.util.Objects;
import java.util.Optional;

public record HudConfigDiagnostic(String code, String message, String source, Optional<Integer> line) {
    public HudConfigDiagnostic {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        source = Objects.requireNonNull(source, "source");
        line = Objects.requireNonNull(line, "line");
    }
}

