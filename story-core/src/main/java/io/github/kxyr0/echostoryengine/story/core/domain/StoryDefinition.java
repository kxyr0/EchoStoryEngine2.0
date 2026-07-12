package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Programmatic story definition for the M1 deterministic runtime slice.
 */
public record StoryDefinition(StoryId storyId, NodeId startNode, Map<NodeId, StoryNode> nodes) {
    public StoryDefinition {
        storyId = Objects.requireNonNull(storyId, "storyId");
        startNode = Objects.requireNonNull(startNode, "startNode");
        nodes = Map.copyOf(Objects.requireNonNull(nodes, "nodes"));
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
}
