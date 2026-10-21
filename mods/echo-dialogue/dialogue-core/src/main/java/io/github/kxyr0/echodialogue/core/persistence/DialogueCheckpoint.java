package io.github.kxyr0.echodialogue.core.persistence;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprint;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Portable stable checkpoint state; it is not a JSON DTO.
 */
public record DialogueCheckpoint(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        CompiledDialogueFingerprint compiledFingerprint,
        AudienceId audienceId,
        DialogueSessionPhase phase,
        int revision,
        DialogueNodeId currentNodeId,
        DialoguePresentationKind presentationKind,
        int lineOrdinal,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        Optional<String> terminalResult,
        Optional<String> cancellationReason,
        Map<String, String> metadata,
        DialogueRepositoryRevision repositoryRevision
) {
    private static final int MAX_METADATA = 16;
    private static final String WAIT_TYPE = "externalWait.type";
    private static final String WAIT_KEY = "externalWait.key";
    private static final String WAIT_PAYLOAD = "externalWait.payload";
    private static final String WAIT_NODE = "externalWait.node";
    private static final String WAIT_POSITION = "externalWait.commandPosition";

    public DialogueCheckpoint {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        compiledFingerprint = Objects.requireNonNull(compiledFingerprint, "compiledFingerprint");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        phase = Objects.requireNonNull(phase, "phase");
        currentNodeId = Objects.requireNonNull(currentNodeId, "currentNodeId");
        presentationKind = Objects.requireNonNull(presentationKind, "presentationKind");
        terminalResult = Objects.requireNonNull(terminalResult, "terminalResult");
        cancellationReason = Objects.requireNonNull(cancellationReason, "cancellationReason");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        repositoryRevision = Objects.requireNonNull(repositoryRevision, "repositoryRevision");
        requireNonNegative(revision, "revision");
        requireNonNegative(lineOrdinal, "lineOrdinal");
        requireNonNegative(createdAtEpochMillis, "createdAtEpochMillis");
        requireNonNegative(updatedAtEpochMillis, "updatedAtEpochMillis");
        if (metadata.size() > MAX_METADATA) {
            throw new IllegalArgumentException("Checkpoint metadata is too large.");
        }
    }

    public DialogueCheckpoint(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            CompiledDialogueFingerprint compiledFingerprint,
            AudienceId audienceId,
            DialogueSessionPhase phase,
            int revision,
            DialogueNodeId currentNodeId,
            int lineOrdinal,
            long createdAtEpochMillis,
            long updatedAtEpochMillis,
            Optional<String> terminalResult,
            Optional<String> cancellationReason,
            Map<String, String> metadata,
            DialogueRepositoryRevision repositoryRevision
    ) {
        this(sessionId, dialogueId, compiledFingerprint, audienceId, phase, revision, currentNodeId,
                DialoguePresentationKind.LINE, lineOrdinal, createdAtEpochMillis, updatedAtEpochMillis,
                terminalResult, cancellationReason, metadata, repositoryRevision);
    }

    public Optional<DialogueOutcomeId> terminalOutcomeId() {
        return terminalResult.map(DialogueOutcomeId::parse);
    }

    public Optional<DialogueExternalWait> externalWait() {
        if (!metadata.containsKey(WAIT_TYPE) || !metadata.containsKey(WAIT_KEY)
                || !metadata.containsKey(WAIT_NODE) || !metadata.containsKey(WAIT_POSITION)) {
            return Optional.empty();
        }
        return Optional.of(new DialogueExternalWait(metadata.get(WAIT_TYPE), metadata.get(WAIT_KEY),
                metadata.getOrDefault(WAIT_PAYLOAD, ""), new DialogueNodeId(metadata.get(WAIT_NODE)),
                Integer.parseInt(metadata.get(WAIT_POSITION))));
    }

    public static DialogueCheckpoint active(
            DialogueSessionState state,
            CompiledDialogue dialogue,
            CompiledDialogueFingerprint fingerprint,
            long nowEpochMillis
    ) {
        return fromState(state, dialogue, fingerprint, nowEpochMillis, TerminalFields.empty());
    }

    public static DialogueCheckpoint terminal(
            DialogueSessionState state,
            CompiledDialogue dialogue,
            CompiledDialogueFingerprint fingerprint,
            long nowEpochMillis,
            TerminalFields terminalFields
    ) {
        return fromState(state, dialogue, fingerprint, nowEpochMillis, terminalFields);
    }

    public DialogueCheckpoint withRepositoryRevision(DialogueRepositoryRevision revision) {
        return new DialogueCheckpoint(sessionId, dialogueId, compiledFingerprint, audienceId, phase,
                this.revision, currentNodeId, presentationKind, lineOrdinal, createdAtEpochMillis, updatedAtEpochMillis,
                terminalResult, cancellationReason, metadata, revision);
    }

    public DialogueCheckpoint withCurrentNodeId(DialogueNodeId nodeId) {
        Map<String, String> nextMetadata = metadata;
        if (metadata.containsKey(WAIT_NODE)) {
            java.util.HashMap<String, String> changed = new java.util.HashMap<>(metadata);
            changed.put(WAIT_NODE, nodeId.toString());
            nextMetadata = Map.copyOf(changed);
        }
        return new DialogueCheckpoint(sessionId, dialogueId, compiledFingerprint, audienceId, phase,
                revision, nodeId, presentationKind, lineOrdinal, createdAtEpochMillis, updatedAtEpochMillis,
                terminalResult, cancellationReason, nextMetadata, repositoryRevision);
    }

    private static DialogueCheckpoint fromState(
            DialogueSessionState state,
            CompiledDialogue dialogue,
            CompiledDialogueFingerprint fingerprint,
            long nowEpochMillis,
            TerminalFields terminalFields
    ) {
        return new DialogueCheckpoint(state.sessionId(), state.dialogueId(), fingerprint, state.audienceId(),
                state.phase(), state.revision(), state.currentNodeId(), state.presentationKind(), state.lineOrdinal(),
                nowEpochMillis, nowEpochMillis, terminalFields.terminalResult(), terminalFields.cancellationReason(),
                metadata(state),
                new DialogueRepositoryRevision(0));
    }

    private static Map<String, String> metadata(DialogueSessionState state) {
        return state.externalWait().map(DialogueCheckpoint::metadata).orElseGet(Map::of);
    }

    private static Map<String, String> metadata(DialogueExternalWait wait) {
        return Map.of(
                WAIT_TYPE, wait.waitType(),
                WAIT_KEY, wait.waitKey(),
                WAIT_PAYLOAD, wait.payload(),
                WAIT_NODE, wait.continuationNodeId().toString(),
                WAIT_POSITION, Integer.toString(wait.commandPosition()));
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
    }

    public record TerminalFields(Optional<String> terminalResult, Optional<String> cancellationReason) {
        public TerminalFields {
            terminalResult = Objects.requireNonNull(terminalResult, "terminalResult");
            cancellationReason = Objects.requireNonNull(cancellationReason, "cancellationReason");
        }

        public static TerminalFields empty() {
            return new TerminalFields(Optional.empty(), Optional.empty());
        }

        public static TerminalFields completed(DialogueOutcomeId value) {
            return new TerminalFields(Optional.of(value.toString()), Optional.empty());
        }

        public static TerminalFields completed(String value) {
            return completed(DialogueOutcomeId.parse(value));
        }

        public static TerminalFields cancelled(String reason) {
            return new TerminalFields(Optional.empty(), Optional.of(reason));
        }
    }
}
