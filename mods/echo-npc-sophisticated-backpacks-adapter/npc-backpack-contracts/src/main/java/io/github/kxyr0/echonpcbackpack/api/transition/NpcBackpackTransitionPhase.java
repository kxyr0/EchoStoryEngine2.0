package io.github.kxyr0.echonpcbackpack.api.transition;

public enum NpcBackpackTransitionPhase {
    RESERVED,
    APPROACHING,
    ALIGNING,
    READY_TO_TAKE,
    PAYLOAD_PERSISTED,
    SOURCE_REMOVED,
    HELD_IN_HAND,
    ATTACHED_TO_BACK,
    READY_TO_PLACE,
    DESTINATION_PLACED,
    COMPLETED,
    FAILED,
    RECOVERY_REQUIRED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == RECOVERY_REQUIRED;
    }
}
