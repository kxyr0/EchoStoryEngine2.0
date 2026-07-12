package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticContext;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.time.Instant;
import java.util.Map;

/**
 * Creates Story source diagnostics for StoryResource semantic binding failures.
 */
final class SemanticDiagnosticFactory {
    private final SourceDiagnosticCatalog catalog = new SourceDiagnosticCatalog();

    StoryDiagnostic create(String code, BindingContext binding, SourceSpan span, Map<String, String> context) {
        SourceDiagnosticContext sourceContext = new SourceDiagnosticContext(
                binding.sourceId(),
                binding.storyId(),
                binding.nodeId(),
                location(span),
                context);
        return catalog.create(code, binding.occurredAt(), sourceContext);
    }

    private SourceLocation location(SourceSpan span) {
        return SourceLocation.of(span.start().line(), span.start().column());
    }
}
