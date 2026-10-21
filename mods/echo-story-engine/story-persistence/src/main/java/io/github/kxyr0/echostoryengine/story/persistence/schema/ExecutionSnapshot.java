package io.github.kxyr0.echostoryengine.story.persistence.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ExecutionSnapshot(
        int saveSchemaVersion,
        String storyId,
        String executionId,
        String definitionFingerprint,
        String definitionRevision,
        int executionRevision,
        String phase,
        String currentNode,
        Map<String, String> variables,
        String pendingActionExecutionId,
        Optional<PendingActionSnapshot> pendingAction,
        String lastCompletedActionExecutionId,
        String failureCode,
        String createdAt,
        String updatedAt) {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;
    public static final String DEFINITION_REVISION = "compiled-story-v1";

    public ExecutionSnapshot {
        if (saveSchemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported save schema version");
        }
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        definitionFingerprint = Objects.requireNonNull(definitionFingerprint, "definitionFingerprint");
        definitionRevision = Objects.requireNonNull(definitionRevision, "definitionRevision");
        phase = Objects.requireNonNull(phase, "phase");
        currentNode = Objects.requireNonNull(currentNode, "currentNode");
        variables = ordered(Objects.requireNonNull(variables, "variables"));
        pendingActionExecutionId = Objects.requireNonNull(pendingActionExecutionId, "pendingActionExecutionId");
        pendingAction = Objects.requireNonNull(pendingAction, "pendingAction");
        lastCompletedActionExecutionId = Objects.requireNonNull(
                lastCompletedActionExecutionId,
                "lastCompletedActionExecutionId");
        failureCode = Objects.requireNonNull(failureCode, "failureCode");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (executionRevision < 0) {
            throw new IllegalArgumentException("execution revision must be non-negative");
        }
    }

    private static Map<String, String> ordered(Map<String, String> source) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
