package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Applies generic external action output bindings declared by compiled story nodes.
 */
final class ExternalActionOutputBinding {
    private final RuntimeDiagnostics diagnostics;

    ExternalActionOutputBinding(RuntimeDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    BindingResult bind(BindingRequest request) {
        StoryExecutionState state = request.state();
        StoryRuntimeInput input = request.input();
        StoryNode node = request.node();
        ActionExecutionId actionId = request.actionId();
        Instant occurredAt = request.occurredAt();
        if (node.outputName().isBlank()) {
            return new BindingResult(state.completedAction(actionId, node.nextNode()), List.of());
        }
        String output = input.completedActionOutputs().get(node.outputName());
        if (output == null) {
            StoryExecutionState failed = state.failedAction(actionId, "ESE-ACTION-006");
            return new BindingResult(failed, List.of(diagnostics.create("ESE-ACTION-006", state, occurredAt,
                    Map.of("output", node.outputName()))));
        }
        StoryExecutionState next = state.completedAction(actionId, node.nextNode(), node.outputName(), output);
        return new BindingResult(next, List.of());
    }

    record BindingRequest(
            StoryExecutionState state,
            StoryRuntimeInput input,
            StoryNode node,
            ActionExecutionId actionId,
            Instant occurredAt) {
        BindingRequest {
            state = Objects.requireNonNull(state, "state");
            input = Objects.requireNonNull(input, "input");
            node = Objects.requireNonNull(node, "node");
            actionId = Objects.requireNonNull(actionId, "actionId");
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }

    record BindingResult(StoryExecutionState state, List<StoryDiagnostic> diagnostics) {
        BindingResult {
            state = Objects.requireNonNull(state, "state");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }
    }
}
