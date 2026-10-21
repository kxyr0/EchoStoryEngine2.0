package io.github.kxyr0.echoplayersleep.api.diagnostic;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import java.util.Objects;

public record PlayerSleepDiagnostic(
        PlayerSleepFailure code,
        PlayerSleepDiagnosticSeverity severity,
        String subject,
        String message) {
    public PlayerSleepDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        subject = Objects.requireNonNull(subject, "subject");
        message = Objects.requireNonNull(message, "message");
    }

    public static PlayerSleepDiagnostic error(PlayerSleepFailure code, String subject, String message) {
        return new PlayerSleepDiagnostic(code, PlayerSleepDiagnosticSeverity.ERROR, subject, message);
    }
}
