package io.github.kxyr0.echoitemattachments.core.recovery;

import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;

public final class RecoveryPlanner {
    public RecoveryDecision decide(ItemCustodyState state, boolean sourceWitnessExists,
            boolean releaseWitnessExists, boolean subjectAvailable) {
        if (sourceWitnessExists && releaseWitnessExists) return RecoveryDecision.QUARANTINE_CONFLICT;
        return switch (state) {
            case WORLD_ITEM -> sourceWitnessExists
                    ? RecoveryDecision.KEEP_WORLD_SOURCE
                    : subjectAvailable ? RecoveryDecision.RESTORE_ATTACHMENT
                    : RecoveryDecision.SPAWN_RELEASE_WITNESS;
            case ATTACHED_TO_SUBJECT -> subjectAvailable
                    ? RecoveryDecision.RESTORE_ATTACHMENT : RecoveryDecision.SPAWN_RELEASE_WITNESS;
            case RELEASING -> releaseWitnessExists
                    ? RecoveryDecision.CONFIRM_EXISTING_RELEASE : RecoveryDecision.SPAWN_RELEASE_WITNESS;
            case WORLD_RELEASED -> releaseWitnessExists
                    ? RecoveryDecision.CONFIRM_EXISTING_RELEASE : RecoveryDecision.QUARANTINE_CONFLICT;
            case RECOVERING -> releaseWitnessExists
                    ? RecoveryDecision.CONFIRM_EXISTING_RELEASE
                    : subjectAvailable ? RecoveryDecision.RESTORE_ATTACHMENT
                    : RecoveryDecision.SPAWN_RELEASE_WITNESS;
        };
    }
}
