package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Objects;

final class NpcActionHandler implements StoryActionHandler {
    private final NamespacedId actionType;
    private final NpcActionStarter starter;

    NpcActionHandler(NamespacedId actionType, NpcActionStarter starter) {
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.starter = Objects.requireNonNull(starter, "starter");
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink) {
        return starter.start(request, completionSink);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink) {
        return starter.recover(request, completionSink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.unsupported("Echo Story NPC Adapter не поддерживает rollback cancellation.");
    }
}
