package io.github.kxyr0.echodialogue.forge.status;

/**
 * Bounded read model for operator-facing dialogue session state.
 */
public record ForgeDialogueHostStatus(
        int activeAttached,
        int activeDetached,
        int retainedCompleted,
        int retainedCancelled,
        int blockedMissingDefinition,
        int blockedIncompatibleFingerprint,
        int duplicateActiveAudienceConflicts,
        int corruptSnapshots,
        int queuedPersistenceOperations,
        int queuedPersistenceCompletions,
        int droppedPersistenceCompletions
) {
    public int activeTotal() {
        return activeAttached + activeDetached;
    }

    public int retainedTerminalTotal() {
        return retainedCompleted + retainedCancelled;
    }

    public int blockedTotal() {
        return blockedMissingDefinition + blockedIncompatibleFingerprint + duplicateActiveAudienceConflicts;
    }
}
