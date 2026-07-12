package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable compiled story definition consumed by the deterministic runtime.
 */
public record StoryDefinition(StoryId storyId, NodeId startNode, Map<NodeId, StoryNode> nodes) {
    public StoryDefinition {
        storyId = Objects.requireNonNull(storyId, "storyId");
        startNode = Objects.requireNonNull(startNode, "startNode");
        nodes = orderedNodes(Objects.requireNonNull(nodes, "nodes"));
        if (!nodes.containsKey(startNode)) {
            throw new IllegalArgumentException("story definition must contain start node");
        }
        nodes.forEach(StoryDefinition::requireMatchingNodeId);
    }

    /**
     * Returns the node with the supplied id when the definition contains it.
     */
    public Optional<StoryNode> find(NodeId nodeId) {
        return Optional.ofNullable(nodes.get(Objects.requireNonNull(nodeId, "nodeId")));
    }

    private static void requireMatchingNodeId(NodeId nodeId, StoryNode node) {
        if (!nodeId.equals(node.id())) {
            throw new IllegalArgumentException("story definition node key must match node id");
        }
    }

    private static Map<NodeId, StoryNode> orderedNodes(Map<NodeId, StoryNode> source) {
        LinkedHashMap<NodeId, StoryNode> ordered = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
