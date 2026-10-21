package io.github.kxyr0.echodialogue.forge.domain;

import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;

public final class DialogueCommitPolicy {
    private DialogueCommitPolicy() { }
    public static boolean requiresSave(DialogueTransitionStatus status) {
        return switch (status) {
            case ADVANCED, WAITING, RESUMED, LOADED, COMPLETED, CANCELLED, FAILED -> true;
            default -> false;
        };
    }
    public static boolean acceptsExternal(DialogueTransitionStatus status) {
        return requiresSave(status) && status != DialogueTransitionStatus.ADVANCED
                && status != DialogueTransitionStatus.LOADED || status == DialogueTransitionStatus.LOAD_REQUESTED;
    }
    public static boolean initial(DialogueTransitionStatus status) {
        return status == DialogueTransitionStatus.STARTED || status == DialogueTransitionStatus.WAITING
                || status == DialogueTransitionStatus.COMPLETED;
    }
}
