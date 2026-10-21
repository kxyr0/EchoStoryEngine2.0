package io.github.kxyr0.echoobservation.core.progress;

import io.github.kxyr0.echoobservation.api.action.ObservationActionContext;
import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutionId;
import io.github.kxyr0.echoobservation.api.action.ObservationActionRegistry;
import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import java.util.Optional;
import java.util.UUID;

public final class ObservationCompletionFlow {
    private final ObservationActionRegistry actions;

    public ObservationCompletionFlow(ObservationActionRegistry actions) {
        this.actions = actions;
    }

    public ObservationCompletionResult complete(UUID playerId, ObservationDefinition definition,
            ObservationPlayerProgress progress) {
        ObservationCompletionResult marked = mark(playerId, definition, progress);
        return execute(playerId, definition, marked.completionId().orElseThrow(), progress,
                marked.status() == ObservationCompletionResult.Status.ALREADY_COMPLETED);
    }

    public ObservationCompletionResult mark(UUID playerId, ObservationDefinition definition,
            ObservationPlayerProgress progress) {
        boolean alreadyCompleted = progress.completed(definition.id()) && !definition.repeatable();
        ObservationCompletionId completionId = progress.completion(definition.id())
                .orElseGet(() -> ObservationCompletionId.deterministic(playerId, definition.id()));
        if (!alreadyCompleted) progress.complete(definition.id(), completionId);
        return result(alreadyCompleted ? ObservationCompletionResult.Status.ALREADY_COMPLETED
                : ObservationCompletionResult.Status.COMPLETED, completionId);
    }

    public ObservationCompletionResult execute(UUID playerId, ObservationDefinition definition,
            ObservationCompletionId completionId, ObservationPlayerProgress progress, boolean alreadyCompleted) {
        for (int index = 0; index < definition.onComplete().actions().size(); index++) {
            var action = definition.onComplete().actions().get(index);
            ObservationActionExecutionId actionId = ObservationActionExecutionId.of(completionId, index);
            if (progress.receipt(actionId)) continue;
            var executor = actions.resolve(action.target());
            if (executor.isEmpty()) return result(ObservationCompletionResult.Status.ACTION_UNAVAILABLE, completionId);
            ObservationActionContext context = new ObservationActionContext(playerId, definition.id(),
                    completionId, actionId);
            if (!executor.orElseThrow().execute(context, action)) {
                return result(ObservationCompletionResult.Status.ACTION_FAILED, completionId);
            }
            progress.acknowledge(actionId);
        }
        return result(alreadyCompleted ? ObservationCompletionResult.Status.ALREADY_COMPLETED
                : ObservationCompletionResult.Status.COMPLETED, completionId);
    }

    private ObservationCompletionResult result(ObservationCompletionResult.Status status,
            ObservationCompletionId id) {
        return new ObservationCompletionResult(status, Optional.of(id));
    }
}
