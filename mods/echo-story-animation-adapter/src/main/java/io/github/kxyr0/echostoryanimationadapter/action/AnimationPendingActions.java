package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationCompletionMapping.Scope;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory server-thread wait registry for terminal animation outcomes.
 */
final class AnimationPendingActions {
    private static final int MAX_PENDING = 1024;
    private final Map<ActionExecutionId, Pending> pending = new LinkedHashMap<>();

    ActionInvocationResult observeOrComplete(
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink,
            AnimationPlaybackQueryResult query
    ) {
        if (query.status() == AnimationPlaybackQueryResult.Status.TERMINAL) {
            AnimationCompletionMapping.terminal(scope, payload, query, completionSink);
            return ActionInvocationResult.accepted();
        }
        if (query.status() == AnimationPlaybackQueryResult.Status.ACTIVE) {
            return observe(scope, payload, completionSink);
        }
        AnimationCompletionMapping.failure(scope, payload, query.status().name(), query.message(), completionSink);
        return ActionInvocationResult.accepted();
    }

    void cancel(ActionExecutionId actionExecutionId) {
        pending.remove(actionExecutionId);
    }

    void tick(AnimationService service) {
        for (Pending item : Map.copyOf(pending).values()) {
            poll(service, item);
        }
    }

    void close() {
        pending.clear();
    }

    private ActionInvocationResult observe(
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
        if (pending.size() >= MAX_PENDING && !pending.containsKey(scope.actionExecutionId())) {
            return ActionInvocationResult.rejected("Echo Story Animation pending action limit reached.");
        }
        pending.put(scope.actionExecutionId(), new Pending(scope, payload, completionSink));
        return ActionInvocationResult.accepted();
    }

    private void poll(AnimationService service, Pending item) {
        AnimationPlaybackQueryResult query = service.queryAnimation(item.payload().subjectId(),
                item.payload().animationId());
        if (query.status() == AnimationPlaybackQueryResult.Status.ACTIVE
                || query.status() == AnimationPlaybackQueryResult.Status.DEFINITION_UNAVAILABLE) {
            return;
        }
        pending.remove(item.scope().actionExecutionId());
        if (query.status() == AnimationPlaybackQueryResult.Status.TERMINAL) {
            AnimationCompletionMapping.terminal(item.scope(), item.payload(), query, item.completionSink());
            return;
        }
        AnimationCompletionMapping.failure(item.scope(), item.payload(), query.status().name(), query.message(),
                item.completionSink());
    }

    private record Pending(
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
    }
}
