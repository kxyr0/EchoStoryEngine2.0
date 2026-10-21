package io.github.kxyr0.echostoryengine.story.api.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;

/**
 * Optional story context attached to a diagnostic.
 *
 * <p>Unknown fields are represented by the empty string instead of {@code null}. This keeps diagnostics friendly
 * to Java callers and avoids optional fields in persisted or transported diagnostic data.
 */
public record DiagnosticSubject(
        String storyId,
        String executionId,
        String nodeId,
        String externalActionId,
        String correlationId,
        String sourceLocation) {
    public DiagnosticSubject {
        storyId = nonNull(storyId, "storyId");
        executionId = nonNull(executionId, "executionId");
        nodeId = nonNull(nodeId, "nodeId");
        externalActionId = nonNull(externalActionId, "externalActionId");
        correlationId = nonNull(correlationId, "correlationId");
        sourceLocation = nonNull(sourceLocation, "sourceLocation");
    }

    /**
     * Creates subject context for a known story execution.
     */
    public static DiagnosticSubject story(StoryId storyId, ExecutionId executionId, NodeId nodeId) {
        return new DiagnosticSubject(
                storyId.asString(),
                executionId.asString(),
                nodeId.value(),
                "",
                executionId.asString(),
                "");
    }

    private static String nonNull(String value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
