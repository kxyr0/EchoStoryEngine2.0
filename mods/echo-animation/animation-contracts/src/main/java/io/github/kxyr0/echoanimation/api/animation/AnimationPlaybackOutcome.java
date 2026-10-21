package io.github.kxyr0.echoanimation.api.animation;

/**
 * Terminal outcome for one high-level animation playback.
 */
public enum AnimationPlaybackOutcome {
    COMPLETED,
    GRACEFULLY_STOPPED,
    EMERGENCY_CANCELLED,
    SUBJECT_OWNER_REMOVED,
    FAILED
}
