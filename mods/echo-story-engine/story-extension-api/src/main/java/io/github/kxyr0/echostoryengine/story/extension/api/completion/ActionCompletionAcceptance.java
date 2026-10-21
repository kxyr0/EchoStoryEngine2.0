package io.github.kxyr0.echostoryengine.story.extension.api.completion;

/**
 * Thread-safe completion sink acceptance category.
 */
public enum ActionCompletionAcceptance {
    ACCEPTED,
    DUPLICATE,
    UNKNOWN_ACTION,
    QUEUE_FULL,
    SHUTDOWN,
    INVALID_REQUEST
}
