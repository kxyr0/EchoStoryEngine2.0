package io.github.kxyr0.echonpc.api.form;

import java.util.Objects;
import java.util.Optional;

/**
 * Public source-linked form diagnostic with stable code text.
 */
public record NpcFormDiagnostic(String code, String message, Optional<String> source) {
    public NpcFormDiagnostic {
        code = text(code, "code");
        message = text(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    public NpcFormDiagnostic(String code, String message, String source) {
        this(code, message, Optional.of(source));
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 256) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }
}
