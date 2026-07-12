package io.github.kxyr0.echostoryengine.story.persistence.recovery;

/**
 * Durable recovery outcome category.
 */
public enum RecoveryStatus {
    RESTORED,
    RECOVERY_REQUESTED,
    TERMINAL,
    REJECTED
}
