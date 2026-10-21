package io.github.kxyr0.echoanimation.api.sync;

/**
 * Result of applying immutable sync data to a client mirror.
 */
public enum AnimationSyncApplyResult {
    APPLIED,
    DUPLICATE,
    STALE,
    REVISION_GAP,
    SUBJECT_REMOVED
}
