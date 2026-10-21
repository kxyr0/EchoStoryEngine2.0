package io.github.kxyr0.echonpc.core.diagnostic;

import java.util.Objects;
import java.util.Optional;

/**
 * Bounded structured diagnostic shared by plain modules and Forge host diagnostics.
 */
public record NpcDiagnostic(
        String code,
        NpcDiagnosticSeverity severity,
        String message,
        String context,
        Optional<Integer> line,
        Optional<Integer> column
) {
    public NpcDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        context = Objects.requireNonNull(context, "context");
        line = Objects.requireNonNull(line, "line");
        column = Objects.requireNonNull(column, "column");
    }

    public static NpcDiagnostic error(String code, String message, String context) {
        return new NpcDiagnostic(code, NpcDiagnosticSeverity.ERROR, message, context, Optional.empty(),
                Optional.empty());
    }

    public static NpcDiagnostic info(String code, String message, String context) {
        return new NpcDiagnostic(code, NpcDiagnosticSeverity.INFO, message, context, Optional.empty(),
                Optional.empty());
    }

    public static NpcDiagnostic warning(String code, String message, String context) {
        return new NpcDiagnostic(code, NpcDiagnosticSeverity.WARNING, message, context, Optional.empty(),
                Optional.empty());
    }

    public boolean error() {
        return severity == NpcDiagnosticSeverity.ERROR;
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name);
        if (checked.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
