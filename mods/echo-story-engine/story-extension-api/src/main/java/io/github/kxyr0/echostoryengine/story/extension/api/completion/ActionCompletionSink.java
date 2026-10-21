package io.github.kxyr0.echostoryengine.story.extension.api.completion;

/**
 * Thread-safe sink used by external handlers to complete actions.
 */
public interface ActionCompletionSink {
    ActionCompletionAcceptance complete(ActionCompletion completion);
}
