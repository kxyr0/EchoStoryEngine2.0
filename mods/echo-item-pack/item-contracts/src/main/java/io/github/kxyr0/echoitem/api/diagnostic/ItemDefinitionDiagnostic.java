package io.github.kxyr0.echoitem.api.diagnostic;

import io.github.kxyr0.echoitem.api.EchoItemId;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable item definition diagnostic for commands, logs, and tests.
 */
public record ItemDefinitionDiagnostic(
        String code,
        ItemDiagnosticSeverity severity,
        String message,
        Optional<EchoItemId> itemId,
        String sourcePath
) {
    public ItemDefinitionDiagnostic {
        code = require(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = require(message, "message");
        itemId = Objects.requireNonNull(itemId, "itemId");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
    }

    public static ItemDefinitionDiagnostic error(String code, String message, String sourcePath) {
        return new ItemDefinitionDiagnostic(code, ItemDiagnosticSeverity.ERROR, message, Optional.empty(), sourcePath);
    }

    public static ItemDefinitionDiagnostic warning(String code, String message, EchoItemId id, String sourcePath) {
        return new ItemDefinitionDiagnostic(code, ItemDiagnosticSeverity.WARNING, message, Optional.of(id), sourcePath);
    }

    public boolean error() {
        return severity == ItemDiagnosticSeverity.ERROR;
    }

    private static String require(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
