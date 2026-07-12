package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable execution position for the minimal runtime state machine.
 *
 * <p>The state is data-only and can be inspected, stored, migrated, and replayed by future ports. It intentionally
 * does not hold callbacks, threads, lambdas, platform objects, or third-party mod objects.
 */
public record StoryExecutionState(
        StoryId storyId,
        ExecutionId executionId,
        NodeId currentNode,
        StoryExecutionPhase phase,
        Map<String, String> variables,
        String pendingActionExecutionId,
        String lastCompletedActionExecutionId,
        String failureCode,
        int revision) {
    public StoryExecutionState {
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        currentNode = Objects.requireNonNull(currentNode, "currentNode");
        phase = Objects.requireNonNull(phase, "phase");
        variables = Map.copyOf(Objects.requireNonNull(variables, "variables"));
        pendingActionExecutionId = Objects.requireNonNull(pendingActionExecutionId, "pendingActionExecutionId");
        lastCompletedActionExecutionId = Objects.requireNonNull(
                lastCompletedActionExecutionId,
                "lastCompletedActionExecutionId");
        failureCode = Objects.requireNonNull(failureCode, "failureCode");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    /**
     * Creates the first state for a known story execution.
     */
    public static StoryExecutionState start(StoryId storyId, ExecutionId executionId) {
        return new StoryExecutionState(
                storyId,
                executionId,
                NodeId.root(),
                StoryExecutionPhase.READY,
                Map.of(),
                "",
                "",
                "",
                0);
    }

    /**
     * Returns a running state at the supplied node.
     */
    public StoryExecutionState runningAt(NodeId nextNode) {
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nextNode, "nextNode"),
                StoryExecutionPhase.RUNNING,
                variables,
                "",
                lastCompletedActionExecutionId,
                "",
                revision + 1);
    }

    /**
     * Returns a running state after changing one story-owned variable.
     */
    public StoryExecutionState withVariable(NodeId nextNode, String variableName, String variableValue) {
        Map<String, String> nextVariables = new HashMap<>(variables);
        nextVariables.put(requireText(variableName, "variableName"), Objects.requireNonNull(variableValue, "value"));
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nextNode, "nextNode"),
                StoryExecutionPhase.RUNNING,
                nextVariables,
                "",
                lastCompletedActionExecutionId,
                "",
                revision + 1);
    }

    /**
     * Returns a waiting state with an explicit pending external action id.
     */
    public StoryExecutionState waitingAt(NodeId nodeId, ActionExecutionId actionExecutionId) {
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nodeId, "nodeId"),
                StoryExecutionPhase.WAITING,
                variables,
                actionExecutionId.asString(),
                lastCompletedActionExecutionId,
                "",
                revision + 1);
    }

    /**
     * Returns a running state after accepting the pending action completion.
     */
    public StoryExecutionState completedAction(ActionExecutionId actionExecutionId, NodeId nextNode) {
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nextNode, "nextNode"),
                StoryExecutionPhase.RUNNING,
                variables,
                "",
                actionExecutionId.asString(),
                "",
                revision + 1);
    }

    /**
     * Returns a failed state after accepting a terminal external action completion.
     */
    public StoryExecutionState failedAction(ActionExecutionId actionExecutionId, String code) {
        return new StoryExecutionState(
                storyId,
                executionId,
                currentNode,
                StoryExecutionPhase.FAILED,
                variables,
                "",
                actionExecutionId.asString(),
                Objects.requireNonNull(code, "code"),
                revision + 1);
    }

    /**
     * Returns a finished state at the supplied node.
     */
    public StoryExecutionState finishedAt(NodeId nodeId) {
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nodeId, "nodeId"),
                StoryExecutionPhase.FINISHED,
                variables,
                "",
                lastCompletedActionExecutionId,
                "",
                revision + 1);
    }

    /**
     * Returns a failed state at the supplied node.
     */
    public StoryExecutionState failedAt(NodeId nodeId, String code) {
        return new StoryExecutionState(
                storyId,
                executionId,
                Objects.requireNonNull(nodeId, "nodeId"),
                StoryExecutionPhase.FAILED,
                variables,
                "",
                lastCompletedActionExecutionId,
                Objects.requireNonNull(code, "code"),
                revision + 1);
    }

    /**
     * Returns whether the supplied action is the current pending external action.
     */
    public boolean isPending(ActionExecutionId actionExecutionId) {
        return pendingActionExecutionId.equals(actionExecutionId.asString());
    }

    /**
     * Returns whether completion for the supplied action id has already been accepted.
     */
    public boolean hasCompleted(ActionExecutionId actionExecutionId) {
        return lastCompletedActionExecutionId.equals(actionExecutionId.asString());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
