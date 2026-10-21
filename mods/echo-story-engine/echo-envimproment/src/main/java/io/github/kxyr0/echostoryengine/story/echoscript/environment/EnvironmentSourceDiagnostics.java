package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.validation.EnvironmentDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class EnvironmentSourceDiagnostics {
    private final EnvironmentDiagnosticFactory factory = new EnvironmentDiagnosticFactory();

    public StoryDiagnostic error(Issue issue) {
        return factory.error(issue.code(), issue.message(), input(issue.source(), issue.span()), issue.context());
    }

    private EnvironmentDiagnosticFactory.DiagnosticInput input(SourceContext source, SourceSpan span) {
        return new EnvironmentDiagnosticFactory.DiagnosticInput(
                source.sourceId(),
                SourceLocation.of(span.start().line(), span.start().column()),
                source.occurredAt());
    }

    public record SourceContext(String sourceId, Instant occurredAt) {
        public SourceContext {
            sourceId = Objects.requireNonNull(sourceId, "sourceId");
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }

    public record Issue(
            String code,
            String message,
            SourceContext source,
            SourceSpan span,
            Map<String, String> context) {
    }
}
