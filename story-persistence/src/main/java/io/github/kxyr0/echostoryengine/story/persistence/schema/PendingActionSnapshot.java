package io.github.kxyr0.echostoryengine.story.persistence.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record PendingActionSnapshot(String actionExecutionId, String actionType, Map<String, String> arguments) {
    public PendingActionSnapshot {
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
        arguments = ordered(Objects.requireNonNull(arguments, "arguments"));
    }

    private static Map<String, String> ordered(Map<String, String> source) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
