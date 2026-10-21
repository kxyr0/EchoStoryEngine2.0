package io.github.kxyr0.echoitemattachments.core.recovery;

public enum RecoveryDecision {
    KEEP_WORLD_SOURCE,
    RESTORE_ATTACHMENT,
    CONFIRM_EXISTING_RELEASE,
    SPAWN_RELEASE_WITNESS,
    QUARANTINE_CONFLICT
}
