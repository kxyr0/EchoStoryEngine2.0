package io.github.kxyr0.echonpcbackpack.core.recovery;

public final class BackpackRecoveryMatrix {
    public BackpackRecoveryDecision equip(boolean sourceExists, boolean payloadPersisted,
            boolean sourceMatchesPayload) {
        if (sourceExists && !payloadPersisted) return BackpackRecoveryDecision.KEEP_SOURCE_AND_FAIL;
        if (!sourceExists && payloadPersisted) return BackpackRecoveryDecision.CONTINUE_WITH_PERSISTED_PAYLOAD;
        if (sourceExists && payloadPersisted && sourceMatchesPayload) {
            return BackpackRecoveryDecision.REMOVE_DUPLICATE_SOURCE_THEN_CONTINUE;
        }
        return BackpackRecoveryDecision.MANUAL_RECOVERY_REQUIRED;
    }

    public BackpackRecoveryDecision unequip(boolean destinationHasExpectedPayload,
            boolean destinationOccupied) {
        if (destinationHasExpectedPayload) return BackpackRecoveryDecision.CONFIRM_DESTINATION_AND_COMPLETE;
        if (destinationOccupied) return BackpackRecoveryDecision.KEEP_ON_NPC_DESTINATION_OCCUPIED;
        return BackpackRecoveryDecision.CONTINUE_WITH_PERSISTED_PAYLOAD;
    }
}
