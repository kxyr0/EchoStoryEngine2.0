package io.github.kxyr0.echoiteminspection.diagnostic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

/** Source-located semantic or syntax failure, including close known item IDs. */
public record InspectionDiagnostic(
        String code, String message, SourceSpan span, boolean error, List<String> suggestions
) {
    public InspectionDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(span, "span");
        suggestions = List.copyOf(suggestions);
    }

    public static InspectionDiagnostic error(String code, String message, SourceSpan span) {
        return new InspectionDiagnostic(code, message, span, true, List.of());
    }

    public String location() {
        return span.sourceId().value() + ":" + span.start().line() + ":" + span.start().column();
    }
}
