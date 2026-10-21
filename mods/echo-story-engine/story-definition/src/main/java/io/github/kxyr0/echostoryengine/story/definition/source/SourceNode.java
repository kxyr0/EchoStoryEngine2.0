package io.github.kxyr0.echostoryengine.story.definition.source;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable raw node data produced by a source adapter before validation.
 */
public record SourceNode(
        Optional<String> id,
        Optional<String> kind,
        Optional<String> nextNode,
        Optional<String> variableName,
        Optional<String> variableValue,
        Optional<String> expectedValue,
        Optional<String> actionType,
        Optional<String> outputName,
        Map<String, String> arguments,
        List<String> invalidArguments,
        Set<String> fieldNames,
        SourceLocation location) {
    public SourceNode {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        nextNode = Objects.requireNonNull(nextNode, "nextNode");
        variableName = Objects.requireNonNull(variableName, "variableName");
        variableValue = Objects.requireNonNull(variableValue, "variableValue");
        expectedValue = Objects.requireNonNull(expectedValue, "expectedValue");
        actionType = Objects.requireNonNull(actionType, "actionType");
        outputName = Objects.requireNonNull(outputName, "outputName");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        invalidArguments = List.copyOf(Objects.requireNonNull(invalidArguments, "invalidArguments"));
        fieldNames = Set.copyOf(Objects.requireNonNull(fieldNames, "fieldNames"));
        location = Objects.requireNonNull(location, "location");
    }
}
