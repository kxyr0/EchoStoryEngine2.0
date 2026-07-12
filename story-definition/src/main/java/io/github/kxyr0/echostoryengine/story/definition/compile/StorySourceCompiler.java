package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticContext;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNodeKind;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compiles validated source into the core runtime story model.
 */
public final class StorySourceCompiler {
    private final SourceDiagnosticCatalog diagnostics = new SourceDiagnosticCatalog();

    public StoryCompileResult compile(SourceStory source, Instant occurredAt) {
        try {
            StoryDefinition compiled = compileValidSource(source);
            return new StoryCompileResult(
                    source.sourceId(),
                    source.formatVersion(),
                    Optional.of(compiled),
                    List.of());
        } catch (IllegalArgumentException exception) {
            StoryDiagnostic diagnostic = diagnostics.create(
                    "ESE-SOURCE-011",
                    occurredAt,
                    SourceDiagnosticContext.fromStory(source, Map.of("detail", exception.getMessage())));
            return new StoryCompileResult(
                    source.sourceId(),
                    source.formatVersion(),
                    Optional.empty(),
                    List.of(diagnostic));
        }
    }

    private StoryDefinition compileValidSource(SourceStory source) {
        Map<NodeId, StoryNode> nodes = new LinkedHashMap<>();
        source.nodes().stream()
                .sorted(java.util.Comparator.comparing(node -> node.id().orElseThrow()))
                .forEach(node -> {
                    NodeId id = NodeId.of(node.id().orElseThrow());
                    nodes.put(id, compileNode(id, node));
                });
        return new StoryDefinition(
                StoryId.of(source.storyId().orElseThrow()),
                NodeId.of(source.startNode().orElseThrow()),
                nodes);
    }

    private StoryNode compileNode(NodeId id, SourceNode node) {
        SourceNodeKind kind = node.kind().flatMap(SourceNodeKind::from).orElseThrow();
        return switch (kind) {
            case START -> StoryNode.start(id, NodeId.of(node.nextNode().orElseThrow()));
            case SET_VARIABLE -> StoryNode.setVariable(
                    id,
                    node.variableName().orElseThrow(),
                    node.variableValue().orElseThrow(),
                    NodeId.of(node.nextNode().orElseThrow()));
            case CONDITION -> StoryNode.condition(
                    id,
                    node.variableName().orElseThrow(),
                    node.expectedValue().orElseThrow(),
                    NodeId.of(node.nextNode().orElseThrow()));
            case EXTERNAL_ACTION -> StoryNode.externalAction(
                    id,
                    NamespacedId.of(node.actionType().orElseThrow()),
                    node.arguments(),
                    NodeId.of(node.nextNode().orElseThrow()));
            case FINISH -> StoryNode.finish(id);
        };
    }
}
