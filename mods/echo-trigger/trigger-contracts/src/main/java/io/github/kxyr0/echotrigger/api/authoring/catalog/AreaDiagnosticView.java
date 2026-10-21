package io.github.kxyr0.echotrigger.api.authoring.catalog;

import java.util.Objects;
import java.util.OptionalInt;

public record AreaDiagnosticView(
        String code,
        String message,
        OptionalInt line,
        OptionalInt column
) {
    public AreaDiagnosticView {
        code = Objects.requireNonNull(code, "code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        line = Objects.requireNonNull(line, "line");
        column = Objects.requireNonNull(column, "column");
        if (code.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("Diagnostic code and message are required.");
        }
    }

    public static AreaDiagnosticView withoutPosition(String code, String message) {
        return new AreaDiagnosticView(code, message, OptionalInt.empty(), OptionalInt.empty());
    }
}
