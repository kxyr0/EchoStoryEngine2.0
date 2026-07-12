package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable compiled story node used by the deterministic runtime.
 */
public record StoryNode(
        NodeId id,
        StoryNodeKind kind,
        NodeId nextNode,
        String variableName,
        String variableValue,
        NamespacedId actionType,
        Map<String, String> actionArguments) {
    private static final NodeId NO_NEXT_NODE = NodeId.of("none");
    private static final NamespacedId NO_ACTION_TYPE = NamespacedId.of("echo_story_engine:no_action");

    public StoryNode {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        nextNode = Objects.requireNonNull(nextNode, "nextNode");
        variableName = Objects.requireNonNull(variableName, "variableName").trim();
        variableValue = Objects.requireNonNull(variableValue, "variableValue");
        actionType = Objects.requireNonNull(actionType, "actionType");
        actionArguments = orderedArguments(Objects.requireNonNull(actionArguments, "actionArguments"));
    }

    /**
     * Creates the explicit start node.
     */
    public static StoryNode start(NodeId id, NodeId nextNode) {
        return new StoryNode(id, StoryNodeKind.START, nextNode, "", "", NO_ACTION_TYPE, Map.of());
    }

    /**
     * Creates a node that writes one story-owned variable.
     */
    public static StoryNode setVariable(NodeId id, String variableName, String variableValue, NodeId nextNode) {
        return new StoryNode(
                id,
                StoryNodeKind.SET_VARIABLE,
                nextNode,
                requireText(variableName, "variableName"),
                Objects.requireNonNull(variableValue, "variableValue"),
                NO_ACTION_TYPE,
                Map.of());
    }

    /**
     * Creates a node that requires one variable to equal one value.
     */
    public static StoryNode condition(NodeId id, String variableName, String expectedValue, NodeId nextNode) {
        return new StoryNode(
                id,
                StoryNodeKind.CONDITION,
                nextNode,
                requireText(variableName, "variableName"),
                Objects.requireNonNull(expectedValue, "expectedValue"),
                NO_ACTION_TYPE,
                Map.of());
    }

    /**
     * Creates a node that requests one abstract external action.
     */
    public static StoryNode externalAction(NodeId id, NamespacedId actionType, Map<String, String> arguments,
            NodeId nextNode) {
        return new StoryNode(
                id,
                StoryNodeKind.EXTERNAL_ACTION,
                nextNode,
                "",
                "",
                Objects.requireNonNull(actionType, "actionType"),
                arguments);
    }

    /**
     * Creates the explicit finish node.
     */
    public static StoryNode finish(NodeId id) {
        return new StoryNode(id, StoryNodeKind.FINISH, NO_NEXT_NODE, "", "", NO_ACTION_TYPE, Map.of());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }

    private static Map<String, String> orderedArguments(Map<String, String> arguments) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        arguments.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
