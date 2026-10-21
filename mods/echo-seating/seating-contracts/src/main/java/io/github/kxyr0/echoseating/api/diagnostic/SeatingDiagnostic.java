package io.github.kxyr0.echoseating.api.diagnostic;

import java.util.Objects;

public record SeatingDiagnostic(SeatingDiagnosticCode code, String subject, String message) {
    public SeatingDiagnostic {
        code = Objects.requireNonNull(code, "code");
        subject = requireText(subject, "subject");
        message = requireText(message, "message");
    }

    public static SeatingDiagnostic error(SeatingDiagnosticCode code, String subject, String message) {
        return new SeatingDiagnostic(code, subject, message);
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
