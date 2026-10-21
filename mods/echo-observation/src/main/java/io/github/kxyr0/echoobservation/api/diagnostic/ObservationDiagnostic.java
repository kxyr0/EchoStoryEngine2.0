package io.github.kxyr0.echoobservation.api.diagnostic;

import java.util.Objects;

public record ObservationDiagnostic(ObservationDiagnosticCode code, Severity severity, String message, int offset) {
    public enum Severity { ERROR, WARNING, HINT }

    public ObservationDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        if (message.isBlank() || offset < 0) {
            throw new IllegalArgumentException("Invalid Observation diagnostic.");
        }
    }

    public boolean error() {
        return severity == Severity.ERROR;
    }
}
