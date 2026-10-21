package io.github.kxyr0.echodialogue.core.session;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit input submitted against a known session revision.
 */
public record DialogueInput(
        DialogueInputKind kind,
        DialogueSessionId sessionId,
        int expectedRevision,
        Optional<ChoiceId> choiceId
) {
    public DialogueInput {
        kind = Objects.requireNonNull(kind, "kind");
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        choiceId = Objects.requireNonNull(choiceId, "choiceId");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("Expected revision must not be negative.");
        }
        if (kind == DialogueInputKind.SELECT_CHOICE && choiceId.isEmpty()) {
            throw new IllegalArgumentException("SELECT_CHOICE input requires ChoiceId.");
        }
    }

    public DialogueInput(DialogueInputKind kind, DialogueSessionId sessionId, int expectedRevision) {
        this(kind, sessionId, expectedRevision, Optional.empty());
    }

    public static DialogueInput advance(DialogueSessionId sessionId, int expectedRevision) {
        return new DialogueInput(DialogueInputKind.ADVANCE, sessionId, expectedRevision);
    }

    public static DialogueInput selectChoice(
            DialogueSessionId sessionId,
            int expectedRevision,
            ChoiceId choiceId
    ) {
        return new DialogueInput(DialogueInputKind.SELECT_CHOICE, sessionId, expectedRevision,
                Optional.of(choiceId));
    }

    public static DialogueInput cancel(DialogueSessionId sessionId, int expectedRevision) {
        return new DialogueInput(DialogueInputKind.CANCEL, sessionId, expectedRevision);
    }
}
