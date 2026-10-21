package io.github.kxyr0.echodialogue.api.wait;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import java.util.Objects;

/**
 * Opaque durable wait descriptor owned by an external adapter.
 */
public record DialogueExternalWait(
        String waitType,
        String waitKey,
        String payload,
        DialogueNodeId continuationNodeId,
        int commandPosition
) {
    public DialogueExternalWait {
        waitType = requireToken(waitType, "waitType");
        waitKey = requireToken(waitKey, "waitKey");
        payload = Objects.requireNonNull(payload, "payload");
        continuationNodeId = Objects.requireNonNull(continuationNodeId, "continuationNodeId");
        if (commandPosition < 0) {
            throw new IllegalArgumentException("commandPosition must not be negative.");
        }
    }

    private static String requireToken(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
