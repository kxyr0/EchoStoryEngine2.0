package io.github.kxyr0.echoplayeractor.api.diagnostic;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import java.util.Objects;
import java.util.Optional;

public record PlayerActorDiagnostic(
        String code,
        DiagnosticSeverity severity,
        String message,
        Optional<PlayerActorId> actorId,
        long tick) {
    public PlayerActorDiagnostic {
        code = Objects.requireNonNull(code, "code").trim();
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        actorId = Objects.requireNonNull(actorId, "actorId");
        if (code.isEmpty()) {
            throw new IllegalArgumentException("code must not be blank.");
        }
    }
}
