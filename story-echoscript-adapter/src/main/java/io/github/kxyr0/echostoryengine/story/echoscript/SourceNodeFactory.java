package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts bound node specifications into neutral source DTOs.
 */
final class SourceNodeFactory {
    Optional<SourceNode> node(IntrinsicCallReader reader, NodeSpec spec) {
        return reader.positionalString(0, spec.kind().jsonName(), "id")
                .map(id -> sourceNode(id, spec));
    }

    SourceNode sourceNode(String id, NodeSpec spec) {
        return new SourceNode(
                Optional.of(id),
                Optional.of(spec.kind().jsonName()),
                spec.next(),
                spec.variable(),
                spec.value(),
                spec.expected(),
                spec.actionType(),
                Map.of(),
                List.of(),
                spec.fields(),
                SourceLocation.of(spec.span().start().line(), spec.span().start().column()));
    }
}
