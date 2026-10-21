package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable compiled dialogue graph.
 */
public record CompiledDialogue(
        DialogueId id,
        DialogueNodeId startNodeId,
        List<CompiledDialogueNode> nodes,
        Map<DialogueNodeId, DialogueNodeId> formerNodeIds,
        Optional<SourceInfo> sourceInfo
) {
    public CompiledDialogue {
        id = Objects.requireNonNull(id, "id");
        startNodeId = Objects.requireNonNull(startNodeId, "startNodeId");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        formerNodeIds = Map.copyOf(Objects.requireNonNull(formerNodeIds, "formerNodeIds"));
        sourceInfo = Objects.requireNonNull(sourceInfo, "sourceInfo");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Compiled dialogue must contain at least one node.");
        }
        Set<DialogueNodeId> ids = new LinkedHashSet<>();
        for (CompiledDialogueNode node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("Dialogue node ID declared more than once: " + node.id());
            }
        }
        if (findNode(startNodeId, nodes).isEmpty()) {
            throw new IllegalArgumentException("Start node is missing from compiled dialogue: " + startNodeId);
        }
        formerNodeIds.forEach((former, current) -> {
            if (ids.contains(former) || !ids.contains(current)) {
                throw new IllegalArgumentException(
                        "Invalid former dialogue node mapping: " + former + " -> " + current);
            }
        });
    }

    public CompiledDialogue(DialogueId id, DialogueNodeId startNodeId, List<CompiledDialogueNode> nodes) {
        this(id, startNodeId, nodes, Map.of(), Optional.empty());
    }

    public CompiledDialogue(DialogueId id, DialogueNodeId startNodeId, List<CompiledDialogueNode> nodes,
            Map<DialogueNodeId, DialogueNodeId> formerNodeIds) {
        this(id, startNodeId, nodes, formerNodeIds, Optional.empty());
    }

    public static CompiledDialogue linear(
            DialogueId id,
            DialogueNodeId startNodeId,
            List<CompiledDialogueLine> lines
    ) {
        return new CompiledDialogue(id, startNodeId, List.of(new CompiledDialogueNode(
                startNodeId,
                lines,
                List.of(),
                Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.DEFAULT)))));
    }

    public Optional<CompiledDialogueNode> findNode(DialogueNodeId nodeId) {
        return findNode(nodeId, nodes);
    }

    public Optional<DialogueNodeId> resolveNodeId(DialogueNodeId savedNodeId) {
        if (findNode(savedNodeId).isPresent()) {
            return Optional.of(savedNodeId);
        }
        return Optional.ofNullable(formerNodeIds.get(savedNodeId));
    }

    public CompiledDialogueNode startNode() {
        return findNode(startNodeId).orElseThrow();
    }

    public List<CompiledDialogueLine> lines() {
        return nodes.stream().flatMap(node -> node.lines().stream()).toList();
    }

    public CompiledDialogueLine lineAt(int index) {
        List<CompiledDialogueLine> orderedLines = lines();
        if (index < 0 || index >= orderedLines.size()) {
            throw new IllegalArgumentException("Line index is outside compiled dialogue: " + index);
        }
        return orderedLines.get(index);
    }

    public boolean isLastLine(int index) {
        return index == lines().size() - 1;
    }

    private static Optional<CompiledDialogueNode> findNode(
            DialogueNodeId nodeId,
            List<CompiledDialogueNode> candidates
    ) {
        return candidates.stream().filter(node -> node.id().equals(nodeId)).findFirst();
    }

    public record SourceInfo(
            String sourceId,
            Optional<String> languageNamespace,
            boolean inferredResourceId,
            boolean highLevelAuthoring,
            Map<DialogueNodeId, NodeSource> nodeSources
    ) {
        public SourceInfo {
            sourceId = Objects.requireNonNull(sourceId, "sourceId");
            languageNamespace = Objects.requireNonNull(languageNamespace, "languageNamespace");
            nodeSources = Map.copyOf(Objects.requireNonNull(nodeSources, "nodeSources"));
        }
    }

    public record NodeSource(String symbol, int line, int column) {
        public NodeSource {
            symbol = Objects.requireNonNull(symbol, "symbol");
            if (line < 1 || column < 1) {
                throw new IllegalArgumentException("Node source line and column must be positive.");
            }
        }
    }
}
