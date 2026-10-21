package io.github.kxyr0.echostoryengine.blackspace.recovery;

public enum BlackspaceLifecycleState {
    IDLE,
    ENTERING,
    ACTIVE,
    EXITING,
    RECOVERY_REQUIRED;

    public boolean requiresRecovery() {
        return this == ENTERING || this == ACTIVE || this == EXITING || this == RECOVERY_REQUIRED;
    }

    public static BlackspaceLifecycleState parse(String value) {
        if (value == null || value.isBlank()) {
            return IDLE;
        }
        try {
            return BlackspaceLifecycleState.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return RECOVERY_REQUIRED;
        }
    }
}
