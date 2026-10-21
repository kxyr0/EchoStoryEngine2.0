package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.forge.status.ForgeDialogueHostStatus;
import java.util.HashSet;
import java.util.Set;

/**
 * Server-thread read model for recovery and retained terminal state.
 */
final class ForgeDialogueStatusTracker {
    private final Set<AudienceId> attachedAudiences = new HashSet<>();
    private int retainedCompleted;
    private int retainedCancelled;
    private int blockedMissingDefinition;
    private int blockedIncompatibleFingerprint;
    private int duplicateActiveAudienceConflicts;
    private int corruptSnapshots;

    void playerAttached(AudienceId audienceId) {
        attachedAudiences.add(audienceId);
    }

    void playerDetached(AudienceId audienceId) {
        attachedAudiences.remove(audienceId);
    }

    void resetRecoveredDurableState() {
        retainedCompleted = 0;
        retainedCancelled = 0;
        blockedMissingDefinition = 0;
        blockedIncompatibleFingerprint = 0;
        duplicateActiveAudienceConflicts = 0;
        corruptSnapshots = 0;
    }

    void terminalRetained(DialogueCheckpoint checkpoint) {
        if (checkpoint.phase() == DialogueSessionPhase.COMPLETED) {
            retainedCompleted++;
        } else if (checkpoint.phase() == DialogueSessionPhase.CANCELLED) {
            retainedCancelled++;
        }
    }

    void blocked(DialogueDiagnostic diagnostic) {
        if (diagnostic.category() == DiagnosticCategory.COMPILED_DIALOGUE_MISSING) {
            blockedMissingDefinition++;
        } else if (diagnostic.category() == DiagnosticCategory.FINGERPRINT_INCOMPATIBLE) {
            blockedIncompatibleFingerprint++;
        }
    }

    void duplicateActiveAudienceConflict() {
        duplicateActiveAudienceConflicts++;
    }

    void corruptSnapshot() {
        corruptSnapshots++;
    }

    ForgeDialogueHostStatus status(ActiveDialogueSessions sessions, ForgePersistenceScheduler scheduler) {
        Set<AudienceId> activeAudiences = sessions.activeAudienceIds();
        int activeAttached = (int) activeAudiences.stream()
                .filter(attachedAudiences::contains)
                .count();
        return new ForgeDialogueHostStatus(
                activeAttached,
                activeAudiences.size() - activeAttached,
                retainedCompleted,
                retainedCancelled,
                blockedMissingDefinition,
                blockedIncompatibleFingerprint,
                duplicateActiveAudienceConflicts,
                corruptSnapshots,
                scheduler.queuedOperations(),
                scheduler.queuedCompletions(),
                scheduler.droppedCompletions());
    }
}
