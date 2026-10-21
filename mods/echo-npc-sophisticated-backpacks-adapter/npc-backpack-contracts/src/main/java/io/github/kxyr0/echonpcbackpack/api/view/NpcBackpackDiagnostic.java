package io.github.kxyr0.echonpcbackpack.api.view;

import java.util.Objects;

public record NpcBackpackDiagnostic(Severity severity, String code, String message) {
    public NpcBackpackDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        if (!code.matches("ENPC-SB-[A-Z0-9-]+") || message.isEmpty() || message.length() > 512) {
            throw new IllegalArgumentException("Diagnostic must have a valid code and bounded message.");
        }
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
