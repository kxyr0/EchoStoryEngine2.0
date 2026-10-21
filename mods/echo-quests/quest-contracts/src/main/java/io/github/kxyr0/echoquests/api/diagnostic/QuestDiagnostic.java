package io.github.kxyr0.echoquests.api.diagnostic;

import io.github.kxyr0.echoquests.api.source.QuestSourceRef;
import java.util.Objects;
import java.util.Optional;

public record QuestDiagnostic(
        QuestDiagnosticCode code,
        Severity severity,
        String message,
        Optional<QuestSourceRef> source) {

    public QuestDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    public boolean error() { return severity == Severity.ERROR; }
    public enum Severity { WARNING, ERROR }
}
