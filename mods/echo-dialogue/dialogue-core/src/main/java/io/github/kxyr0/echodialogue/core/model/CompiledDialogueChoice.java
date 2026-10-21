package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import java.util.Objects;

/**
 * Immutable compiled choice with server-only branch target.
 */
public record CompiledDialogueChoice(
        ChoiceId id,
        String text,
        DialogueNodeId targetNodeId
) {
    public CompiledDialogueChoice {
        id = Objects.requireNonNull(id, "id");
        text = requireText(text, "text");
        targetNodeId = Objects.requireNonNull(targetNodeId, "targetNodeId");
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
