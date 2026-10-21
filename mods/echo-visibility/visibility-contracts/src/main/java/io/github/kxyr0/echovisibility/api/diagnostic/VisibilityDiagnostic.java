package io.github.kxyr0.echovisibility.api.diagnostic;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.Objects;
import java.util.Optional;

public record VisibilityDiagnostic(
        VisibilityDiagnosticCode code,
        String message,
        Optional<VisibilityWatchId> watchId,
        Optional<VisibilityWatchGeneration> generation,
        Optional<String> source) {
    public VisibilityDiagnostic {
        code = Objects.requireNonNull(code, "code");
        message = requireText(message, "message");
        watchId = Objects.requireNonNull(watchId, "watchId");
        generation = Objects.requireNonNull(generation, "generation");
        source = Objects.requireNonNull(source, "source").map(value -> requireText(value, "source"));
    }

    public static VisibilityDiagnostic of(VisibilityDiagnosticCode code, String message) {
        return new VisibilityDiagnostic(code, message, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 1024) {
            throw new IllegalArgumentException(name + " must contain 1..1024 characters.");
        }
        return checked;
    }
}
