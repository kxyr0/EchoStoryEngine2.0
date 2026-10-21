package io.github.kxyr0.echostoryengine.story.forge.host.readmodel;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeDispatch;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeSavePlan;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded server-thread read model for command-visible execution summaries.
 */
public final class ExecutionReadModel {
    private final int limit;
    private final Map<ExecutionId, ExecutionSummary> executions = new LinkedHashMap<>();

    public ExecutionReadModel(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    public void update(StoryRuntimeSavePlan plan, StoryRuntimeDispatch dispatch) {
        executions.put(dispatch.state().executionId(), ExecutionSummary.from(dispatch.state(),
                pendingActionType(plan, dispatch)));
        while (executions.size() > limit) {
            ExecutionId first = executions.keySet().iterator().next();
            executions.remove(first);
        }
    }

    public List<ExecutionSummary> summaries() {
        return executions.values().stream()
                .sorted((left, right) -> left.executionId().asString().compareTo(right.executionId().asString()))
                .toList();
    }

    public int waitingCount() {
        return (int) executions.values().stream()
                .filter(summary -> summary.phase() == StoryExecutionPhase.WAITING)
                .count();
    }

    private String pendingActionType(StoryRuntimeSavePlan plan, StoryRuntimeDispatch dispatch) {
        if (dispatch.state().phase() != StoryExecutionPhase.WAITING) {
            return "";
        }
        return plan.checkpoint()
                .flatMap(checkpoint -> checkpoint.pendingAction().map(PendingActionCheckpoint::actionType))
                .map(Object::toString)
                .orElse("");
    }
}
