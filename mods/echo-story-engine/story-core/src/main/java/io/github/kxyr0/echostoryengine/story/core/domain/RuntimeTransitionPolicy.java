package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RuntimeTransitionPolicy {
    private final StoryDefinition definition;
    private final Set<NamespacedId> supportedActionTypes;
    private final int transitionBudget;
    private final RuntimeDiagnostics diagnosticCatalog = new RuntimeDiagnostics();
    private final ExternalActionOutputBinding outputBinding = new ExternalActionOutputBinding(diagnosticCatalog);

    public RuntimeTransitionPolicy(StoryDefinition definition, Set<NamespacedId> supportedActionTypes,
                                   int transitionBudget) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.supportedActionTypes = Set.copyOf(Objects.requireNonNull(supportedActionTypes, "supportedActionTypes"));
        if (transitionBudget < 1) { throw new IllegalArgumentException("transitionBudget must be positive"); }
        this.transitionBudget = transitionBudget;
    }

    public StoryRuntimeResult advance(StoryExecutionState current, StoryRuntimeInput input, Instant occurredAt) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(occurredAt, "occurredAt");
        StoryExecutionState state = Objects.requireNonNull(current, "current");
        StoryRuntimeResult completion = input.isExternalActionCompletion()
                ? acceptCompletion(state, input, occurredAt)
                : result(startIfReady(state));
        StoryRuntimeResult runtime = runIfPossible(completion.state(), occurredAt);
        return merge(input, completion, runtime);
    }

    public CompiledStoryFingerprint definitionFingerprint() { return StoryDefinitionFingerprint.from(definition); }

    private StoryRuntimeResult acceptCompletion(
            StoryExecutionState state,
            StoryRuntimeInput input,
            Instant occurredAt) {
        ActionExecutionId actionId = input.completedActionExecutionId();
        if (state.hasCompleted(actionId)) {
            return result(state, List.of(), List.of(diag("ESE-ACTION-003", state, occurredAt,
                    Map.of("actionExecutionId", actionId.asString()))));
        }
        if (state.phase() != StoryExecutionPhase.WAITING || !state.isPending(actionId)) {
            return result(state, List.of(), List.of(diag("ESE-ACTION-004", state, occurredAt,
                    Map.of("actionExecutionId", actionId.asString()))));
        }
        if (input.completedActionResultCategory() != ExternalActionResultCategory.SUCCESS) {
            StoryExecutionState failed = state.failedAction(actionId, "ESE-ACTION-005");
            return result(failed, List.of(), List.of(diag("ESE-ACTION-005", state, occurredAt,
                    Map.of("resultCategory", input.completedActionResultCategory().name()))));
        }
        Optional<StoryNode> node = definition.find(state.currentNode());
        if (node.isEmpty() || node.get().kind() != StoryNodeKind.EXTERNAL_ACTION) {
            StoryExecutionState failed = state.failedAt(state.currentNode(), "ESE-RUNTIME-004");
            return result(failed, List.of(), List.of(diag("ESE-RUNTIME-004", state, occurredAt,
                    Map.of("nodeId", state.currentNode().value()))));
        }
        StoryNode currentNode = node.get();
        ExternalActionOutputBinding.BindingResult binding = outputBinding.bind(
                new ExternalActionOutputBinding.BindingRequest(state, input, currentNode, actionId, occurredAt));
        return result(binding.state(), List.of(), binding.diagnostics());
    }

    private StoryRuntimeResult runIfPossible(StoryExecutionState state, Instant occurredAt) {
        if (state.phase() != StoryExecutionPhase.RUNNING) { return result(state); }
        return runSynchronous(state, occurredAt);
    }

    private StoryRuntimeResult runSynchronous(StoryExecutionState state, Instant occurredAt) {
        StoryExecutionState current = state;
        List<ExternalCommand> commands = new ArrayList<>();
        List<StoryDiagnostic> diagnostics = new ArrayList<>();
        for (int index = 0; index < transitionBudget; index++) {
            if (current.phase() != StoryExecutionPhase.RUNNING) {
                return result(current, commands, diagnostics);
            }
            Optional<StoryNode> node = definition.find(current.currentNode());
            if (node.isEmpty()) {
                StoryExecutionState failed = current.failedAt(current.currentNode(), "ESE-RUNTIME-004");
                diagnostics.add(diag("ESE-RUNTIME-004", current, occurredAt,
                        Map.of("nodeId", current.currentNode().value())));
                return result(failed, commands, diagnostics);
            }
            StoryRuntimeResult nodeResult = applyNode(current, node.get(), occurredAt);
            commands.addAll(nodeResult.commands());
            diagnostics.addAll(nodeResult.diagnostics());
            current = nodeResult.state();
        }
        StoryExecutionState failed = current.failedAt(current.currentNode(), "ESE-RUNTIME-002");
        diagnostics.add(diag("ESE-RUNTIME-002", current, occurredAt,
                Map.of("budget", Integer.toString(transitionBudget))));
        return result(failed, commands, diagnostics);
    }

    private StoryRuntimeResult applyNode(StoryExecutionState state, StoryNode node, Instant occurredAt) {
        return switch (node.kind()) {
            case START -> result(state.runningAt(node.nextNode()));
            case SET_VARIABLE -> result(state.withVariable(node.nextNode(), node.variableName(), node.variableValue()));
            case CONDITION -> applyCondition(state, node, occurredAt);
            case EXTERNAL_ACTION -> applyExternalAction(state, node, occurredAt);
            case FINISH -> {
                StoryExecutionState finished = state.finishedAt(node.id());
                yield result(finished, List.of(), List.of(diag("ESE-RUNTIME-005", finished, occurredAt, Map.of())));
            }
        };
    }

    private StoryRuntimeResult applyCondition(StoryExecutionState state, StoryNode node, Instant occurredAt) {
        String actualValue = state.variables().get(node.variableName());
        if (node.variableValue().equals(actualValue)) {
            return result(state.runningAt(node.nextNode()));
        }
        StoryExecutionState failed = state.failedAt(node.id(), "ESE-RUNTIME-003");
        return result(failed, List.of(), List.of(diag("ESE-RUNTIME-003", state, occurredAt,
                Map.of("variableName", node.variableName(), "expectedValue", node.variableValue()))));
    }

    private StoryRuntimeResult applyExternalAction(StoryExecutionState state, StoryNode node, Instant occurredAt) {
        if (!supportedActionTypes.contains(node.actionType())) {
            StoryExecutionState failed = state.failedAt(node.id(), "ESE-ACTION-001");
            return result(failed, List.of(), List.of(diag("ESE-ACTION-001", state, occurredAt,
                    Map.of("actionType", node.actionType().asString()))));
        }
        ActionExecutionId actionId = actionExecutionId(state.executionId(), node.id());
        ExternalCommand command = new ExternalCommand(
                node.actionType(),
                actionId,
                resolvePayload(node.actionArguments(), state.variables()));
        StoryExecutionState waiting = state.waitingAt(node.id(), actionId);
        return result(waiting, List.of(command), List.of(diag("ESE-ACTION-002", waiting, occurredAt,
                Map.of("actionType", node.actionType().asString(), "actionExecutionId", actionId.asString()))));
    }

    private Map<String, String> resolvePayload(Map<String, String> payload, Map<String, String> variables) {
        Map<String, String> resolved = new LinkedHashMap<>();
        payload.forEach((key, value) -> resolved.put(key, variables.getOrDefault(value, value)));
        return Map.copyOf(resolved);
    }

    private StoryExecutionState startIfReady(StoryExecutionState state) {
        return state.phase() == StoryExecutionPhase.READY ? state.runningAt(definition.startNode()) : state;
    }

    private StoryRuntimeResult merge(
            StoryRuntimeInput input,
            StoryRuntimeResult completion,
            StoryRuntimeResult runtime) {
        List<ExternalCommand> commands = new ArrayList<>(completion.commands());
        List<StoryDiagnostic> diagnostics = new ArrayList<>(completion.diagnostics());
        commands.addAll(runtime.commands());
        diagnostics.addAll(runtime.diagnostics());
        return new StoryRuntimeResult(input.executionId(), runtime.state(), commands, diagnostics);
    }

    private StoryRuntimeResult result(StoryExecutionState state) {
        return result(state, List.of(), List.of());
    }

    private StoryRuntimeResult result(
            StoryExecutionState state,
            List<ExternalCommand> commands,
            List<StoryDiagnostic> diagnostics) {
        return new StoryRuntimeResult(state.executionId(), state, commands, diagnostics);
    }

    private ActionExecutionId actionExecutionId(ExecutionId executionId, NodeId nodeId) {
        String namespace = executionId.value().namespace();
        String path = executionId.value().path() + "/" + nodeId.value();
        return ActionExecutionId.of(namespace + ":" + path);
    }

    private StoryDiagnostic diag(
            String code,
            StoryExecutionState state,
            Instant occurredAt,
            Map<String, String> context) {
        return diagnosticCatalog.create(code, state, occurredAt, context);
    }
}
