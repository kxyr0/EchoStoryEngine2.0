package io.github.kxyr0.echodialogue.persistence.schema;

import java.util.Map;

/**
 * Explicit persisted save schema version 1.
 */
public record DialogueSaveV1(
        int saveSchemaVersion,
        String sessionId,
        String dialogueId,
        String compiledFingerprint,
        String audienceId,
        String phase,
        int revision,
        String currentNodeId,
        int lineOrdinal,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        String terminalResult,
        String cancellationReason,
        Map<String, String> metadata,
        long repositoryRevision
) {
    public static final int VERSION = 1;
}
