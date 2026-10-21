package io.github.kxyr0.echodialogue.api.diagnostic;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional source or runtime object pointed to by a diagnostic.
 */
public record DiagnosticSubject(
        String kind,
        String value,
        Optional<Integer> line,
        Optional<Integer> column
) {
    public DiagnosticSubject(String kind, String value) {
        this(kind, value, Optional.empty(), Optional.empty());
    }

    public DiagnosticSubject {
        kind = Objects.requireNonNull(kind, "kind").trim();
        value = Objects.requireNonNull(value, "value").trim();
        line = Objects.requireNonNull(line, "line");
        column = Objects.requireNonNull(column, "column");
        if (kind.isEmpty() || value.isEmpty()) {
            throw new IllegalArgumentException("Diagnostic subject kind and value must be non-empty.");
        }
        line.ifPresent(DiagnosticSubject::requireOneBased);
        column.ifPresent(DiagnosticSubject::requireOneBased);
    }

    public static Optional<DiagnosticSubject> none() {
        return Optional.empty();
    }

    public static Optional<DiagnosticSubject> source(String sourceId, int line, int column) {
        return Optional.of(new DiagnosticSubject("source", sourceId, Optional.of(line), Optional.of(column)));
    }

    public static Optional<DiagnosticSubject> source(String sourceId) {
        return Optional.of(new DiagnosticSubject("source", sourceId));
    }

    private static void requireOneBased(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Diagnostic source coordinates must be one-based.");
        }
    }
}
