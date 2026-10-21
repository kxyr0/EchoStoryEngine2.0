package io.github.kxyr0.echotrigger.api.model;

import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;

import java.util.Objects;

public record TriggerDiagnostic(
        TriggerDiagnosticCode code,
        String message,
        java.util.Optional<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition.Location>
                source) {
    public TriggerDiagnostic(TriggerDiagnosticCode code, String message) {
        this(code, message, java.util.Optional.empty());
    }

    public TriggerDiagnostic {
        code = Objects.requireNonNull(code, "code");
        source = Objects.requireNonNull(source, "source");
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Diagnostic message must not be blank.");
        }
    }

    public boolean isError() {
        return true;
    }
}
