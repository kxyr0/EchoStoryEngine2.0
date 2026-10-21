package io.github.kxyr0.echodialogue.core.session;

/**
 * Exact outcome of one session transition.
 */
public enum DialogueTransitionStatus {
    STARTED,
    ADVANCED,
    WAITING,
    RESUMED,
    LOAD_REQUESTED,
    LOADED,
    COMPLETED,
    CANCELLED,
    IGNORED,
    REJECTED,
    FAILED
}
