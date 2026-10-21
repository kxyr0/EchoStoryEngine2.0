package io.github.kxyr0.echodialogue.api.session;

/**
 * Public session status visible to external integrations.
 */
public enum DialogueSessionStatus {
    ACTIVE,
    WAITING,
    COMPLETED,
    CANCELLED,
    FAILED,
    BLOCKED
}
