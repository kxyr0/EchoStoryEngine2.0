package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure transition policy for the minimal explicit state machine.
 */
public final class RuntimeTransitionPolicy {
    public StoryRuntimeResult advance(StoryExecutionState current, StoryRuntimeInput input, Instant occurredAt) {
        Objects.requireNonNull(input, "input");
        StoryExecutionState checkedState = Objects.requireNonNull(current, "current");
        StoryExecutionState nextState = checkedState.advanceTo(NodeId.of("runtime.accepted"));
        StoryDiagnostic diagnostic = new StoryDiagnostic(
                "ESE-RUNTIME-001",
                DiagnosticSeverity.INFO,
                "runtime",
                "Runtime input accepted",
                DiagnosticSubject.story(nextState.storyId(), nextState.executionId(), nextState.currentNode()),
                Map.of("eventType", input.eventType().asString()),
                "state-transition",
                Objects.requireNonNull(occurredAt, "occurredAt"),
                "");
        return new StoryRuntimeResult(input.executionId(), nextState, List.of(), List.of(diagnostic));
    }
}
