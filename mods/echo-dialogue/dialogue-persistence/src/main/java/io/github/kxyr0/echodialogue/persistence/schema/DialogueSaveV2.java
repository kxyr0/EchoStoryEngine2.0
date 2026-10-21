package io.github.kxyr0.echodialogue.persistence.schema;

import java.util.Map;

/**
 * Explicit persisted save schema version 2 with presentation kind and terminal outcome.
 */
public record DialogueSaveV2(
        int saveSchemaVersion,
        String sessionId,
        String dialogueId,
        String compiledFingerprint,
        String audienceId,
        String phase,
        int revision,
        String currentNodeId,
        String presentationKind,
        int lineOrdinal,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        String terminalOutcomeId,
        String cancellationReason,
        Map<String, String> metadata,
        long repositoryRevision
) {
    public static final int VERSION = 2;
}
