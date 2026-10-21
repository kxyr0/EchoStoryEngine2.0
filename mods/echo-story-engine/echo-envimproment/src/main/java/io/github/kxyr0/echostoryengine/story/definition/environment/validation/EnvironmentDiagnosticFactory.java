package io.github.kxyr0.echostoryengine.story.definition.environment.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Creates source diagnostics for EnvironmentResource compilation.
 */
public final class EnvironmentDiagnosticFactory {
    public StoryDiagnostic error(
            String code,
            String message,
            DiagnosticInput input,
            Map<String, String> context) {
        return new StoryDiagnostic(
                Objects.requireNonNull(code, "code"),
                DiagnosticSeverity.ERROR,
                "source",
                Objects.requireNonNull(message, "message"),
                subject(input),
                Map.copyOf(Objects.requireNonNull(context, "context")),
                "environment-source",
                input.occurredAt(),
                "Fix the EnvironmentResource and run reload again.");
    }

    private DiagnosticSubject subject(DiagnosticInput input) {
        SourceLocation location = input.location();
        return new DiagnosticSubject("", "", "", "", input.sourceId(),
                location.isKnown() ? location.asSubjectValue() : input.sourceId());
    }

    public record DiagnosticInput(String sourceId, SourceLocation location, Instant occurredAt) {
        public DiagnosticInput {
            sourceId = Objects.requireNonNull(sourceId, "sourceId");
            location = Objects.requireNonNull(location, "location");
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }
}
