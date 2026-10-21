package io.github.kxyr0.echostoryengine.story.extension.api.execution;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;

/**
 * Server-scoped handler for one external action type.
 */
public interface StoryActionHandler {
    NamespacedId actionType();

    ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink);

    ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink);

    ActionInvocationResult cancel(ActionCancelRequest request);
}
