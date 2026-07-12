package io.github.kxyr0.echostoryengine.story.core.port;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded durable data needed to recover one pending external action.
 */
public record PendingActionCheckpoint(
        ActionExecutionId actionExecutionId,
        NamespacedId actionType,
        Map<String, String> arguments) {
    public PendingActionCheckpoint {
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
        arguments = ordered(Objects.requireNonNull(arguments, "arguments"));
    }

    /**
     * Captures the durable recovery data from a command that is about to be dispatched.
     */
    public static PendingActionCheckpoint from(ExternalCommand command) {
        Objects.requireNonNull(command, "command");
        return new PendingActionCheckpoint(
                command.actionExecutionId(),
                command.actionType(),
                command.payload());
    }

    /**
     * Returns whether this checkpoint belongs to the supplied action id string.
     */
    public boolean matches(String actionExecutionId) {
        return this.actionExecutionId.asString().equals(actionExecutionId);
    }

    private static Map<String, String> ordered(Map<String, String> source) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
