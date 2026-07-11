package io.github.kxyr0.echostoryengine.story.api.diagnostic;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Structured diagnostic emitted by the story runtime.
 *
 * <p>Codes are stable and searchable. Context is bounded key/value data; adapters must avoid dumping large story
 * state or sensitive values. The type is immutable and safe to retain in bounded diagnostic buffers.
 */
public record StoryDiagnostic(
        String code,
        DiagnosticSeverity severity,
        String subsystem,
        String message,
        DiagnosticSubject subject,
        Map<String, String> context,
        String causeCategory,
        Instant occurredAt,
        String operatorHint) {
    public StoryDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        subsystem = requireText(subsystem, "subsystem");
        message = requireText(message, "message");
        subject = Objects.requireNonNull(subject, "subject");
        context = Map.copyOf(Objects.requireNonNull(context, "context"));
        causeCategory = requireText(causeCategory, "causeCategory");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        operatorHint = Objects.requireNonNull(operatorHint, "operatorHint");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
