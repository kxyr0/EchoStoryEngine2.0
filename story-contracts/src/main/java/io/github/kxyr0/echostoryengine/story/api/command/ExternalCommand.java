package io.github.kxyr0.echostoryengine.story.api.command;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract command requested by the story runtime.
 *
 * <p>The command identifies an external action type without knowing whether an adapter will move an entity,
 * show UI, call another mod, or do nothing. Payload values must stay small and serializable as neutral strings.
 */
public record ExternalCommand(NamespacedId actionType, String operationId, Map<String, String> payload) {
    public ExternalCommand {
        actionType = Objects.requireNonNull(actionType, "actionType");
        operationId = requireText(operationId, "operationId");
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
