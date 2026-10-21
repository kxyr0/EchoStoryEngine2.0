package io.github.kxyr0.echostoryengine.story.definition.source;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable raw story data produced by a source adapter before validation.
 */
public record SourceStory(
        String sourceId,
        Optional<Integer> formatVersion,
        Optional<String> storyId,
        Optional<String> startNode,
        List<SourceNode> nodes,
        Set<String> fieldNames,
        SourceLocation location) {
    public SourceStory {
        sourceId = requireText(sourceId, "sourceId");
        formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
        storyId = Objects.requireNonNull(storyId, "storyId");
        startNode = Objects.requireNonNull(startNode, "startNode");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        fieldNames = Set.copyOf(Objects.requireNonNull(fieldNames, "fieldNames"));
        location = Objects.requireNonNull(location, "location");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
