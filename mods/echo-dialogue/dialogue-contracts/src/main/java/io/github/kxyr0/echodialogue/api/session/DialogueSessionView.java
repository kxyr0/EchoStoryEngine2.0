package io.github.kxyr0.echodialogue.api.session;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable public read model for one dialogue session.
 */
public record DialogueSessionView(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        AudienceId audienceId,
        DialogueSessionStatus status,
        int revision,
        long repositoryRevision,
        Optional<String> terminalResult,
        Optional<String> cancellationReason,
        Optional<String> blockedReason,
        Optional<DialogueExternalWait> externalWait
) {
    public DialogueSessionView {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        status = Objects.requireNonNull(status, "status");
        terminalResult = Objects.requireNonNull(terminalResult, "terminalResult");
        cancellationReason = Objects.requireNonNull(cancellationReason, "cancellationReason");
        blockedReason = Objects.requireNonNull(blockedReason, "blockedReason");
        externalWait = Objects.requireNonNull(externalWait, "externalWait");
        if (revision < 0 || repositoryRevision < 0) {
            throw new IllegalArgumentException("Session revisions must not be negative.");
        }
    }

    public DialogueSessionView(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            AudienceId audienceId,
            DialogueSessionStatus status,
            int revision,
            long repositoryRevision,
            Optional<String> terminalResult,
            Optional<String> cancellationReason,
            Optional<String> blockedReason
    ) {
        this(sessionId, dialogueId, audienceId, status, revision, repositoryRevision,
                terminalResult, cancellationReason, blockedReason, Optional.empty());
    }

    public Optional<DialogueOutcomeId> terminalOutcomeId() {
        return terminalResult.map(DialogueOutcomeId::parse);
    }
}
