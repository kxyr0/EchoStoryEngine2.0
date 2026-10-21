package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import java.util.Objects;

/**
 * Immutable terminal completion declared by a dialogue node.
 */
public record CompiledDialogueCompletion(DialogueOutcomeId outcomeId) {
    public CompiledDialogueCompletion {
        outcomeId = Objects.requireNonNull(outcomeId, "outcomeId");
    }
}
