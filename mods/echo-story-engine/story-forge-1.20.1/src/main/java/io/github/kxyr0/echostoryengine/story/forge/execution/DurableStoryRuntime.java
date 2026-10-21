package io.github.kxyr0.echostoryengine.story.forge.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveStatus;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Split orchestration: transition on server thread, file load/save through persistence queue.
 */
public final class DurableStoryRuntime {
    private static final int TRANSITION_BUDGET = 16;

    private final ForgeRuntimeDiagnostics diagnostics;

    public DurableStoryRuntime(ForgeRuntimeDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public StoryRuntimeSavePlan afterLoad(
            StoryRuntimeInput input,
            CompiledStoryCatalogEntry entry,
            ExecutionLoadResult loaded,
            Instant occurredAt) {
        if (loaded.status() == ExecutionLoadStatus.FAILED) {
            return failedPlan(input, entry, loaded);
        }
        LoadedCheckpoint current = currentCheckpoint(input, entry, loaded);
        if (!current.checkpoint().definitionFingerprint().equals(entry.fingerprint())) {
            return incompatiblePlan(input, entry, new IncompatibleLoad(current.checkpoint(), loaded, occurredAt));
        }
        StoryRuntimeResult transition = policy(entry).advance(current.checkpoint().state(), input, occurredAt);
        ExecutionCheckpoint next = nextCheckpoint(current.checkpoint(), entry, transition);
        return new StoryRuntimeSavePlan(
                input,
                entry,
                Optional.of(next),
                current.expectedRevision(),
                Optional.of(transition),
                current.checkpoint().state(),
                loaded.diagnostics());
    }

    public StoryRuntimeDispatch afterSave(
            StoryRuntimeSavePlan plan,
            ExecutionSaveResult saved,
            Instant occurredAt) {
        StoryRuntimeResult transition = plan.transition().orElseThrow();
        List<StoryDiagnostic> allDiagnostics = new ArrayList<>(plan.diagnostics());
        if (saved.status() == ExecutionSaveStatus.SAVED) {
            allDiagnostics.addAll(transition.diagnostics());
            allDiagnostics.addAll(saved.diagnostics());
            return new StoryRuntimeDispatch(
                    plan.catalogEntry(),
                    transition.state(),
                    transition.commands(),
                    allDiagnostics,
                    true);
        }
        allDiagnostics.addAll(saved.diagnostics());
        if (!transition.commands().isEmpty()) {
            allDiagnostics.add(diagnostics.externalCommandSuppressed(plan.stateBeforeTransition(), occurredAt));
        }
        return new StoryRuntimeDispatch(
                plan.catalogEntry(),
                plan.stateBeforeTransition(),
                List.of(),
                allDiagnostics,
                false);
    }

    private StoryRuntimeSavePlan failedPlan(
            StoryRuntimeInput input,
            CompiledStoryCatalogEntry entry,
            ExecutionLoadResult loaded) {
        StoryExecutionState failed = StoryExecutionState.start(input.storyId(), input.executionId())
                .failedAt(io.github.kxyr0.echostoryengine.story.api.identity.NodeId.root(), "ESE-PERSISTENCE-002");
        return new StoryRuntimeSavePlan(input, entry, Optional.empty(), -1, Optional.empty(), failed,
                loaded.diagnostics());
    }

    private StoryRuntimeSavePlan incompatiblePlan(
            StoryRuntimeInput input,
            CompiledStoryCatalogEntry entry,
            IncompatibleLoad incompatible) {
        List<StoryDiagnostic> allDiagnostics = new ArrayList<>(incompatible.loaded().diagnostics());
        allDiagnostics.add(diagnostics.incompatibleDefinition(incompatible.checkpoint().state(),
                incompatible.occurredAt()));
        return new StoryRuntimeSavePlan(input, entry, Optional.empty(), -1, Optional.empty(),
                incompatible.checkpoint().state(),
                allDiagnostics);
    }

    private LoadedCheckpoint currentCheckpoint(
            StoryRuntimeInput input,
            CompiledStoryCatalogEntry entry,
            ExecutionLoadResult loaded) {
        if (loaded.status() == ExecutionLoadStatus.FOUND) {
            ExecutionCheckpoint checkpoint = loaded.checkpoint().orElseThrow();
            return new LoadedCheckpoint(checkpoint, checkpoint.state().revision());
        }
        StoryExecutionState state = StoryExecutionState.start(input.storyId(), input.executionId());
        return new LoadedCheckpoint(ExecutionCheckpoint.withoutPending(state, entry.fingerprint()), -1);
    }

    private ExecutionCheckpoint nextCheckpoint(
            ExecutionCheckpoint current,
            CompiledStoryCatalogEntry entry,
            StoryRuntimeResult transition) {
        return new ExecutionCheckpoint(transition.state(), entry.fingerprint(), pendingAction(current, transition));
    }

    private Optional<PendingActionCheckpoint> pendingAction(
            ExecutionCheckpoint current,
            StoryRuntimeResult transition) {
        if (transition.state().pendingActionExecutionId().isBlank()) {
            return Optional.empty();
        }
        Optional<PendingActionCheckpoint> fromCommand = transition.commands().stream()
                .filter(command -> matchesPending(transition.state(), command))
                .findFirst()
                .map(PendingActionCheckpoint::from);
        return fromCommand.or(() -> current.pendingAction()
                .filter(pending -> pending.matches(transition.state().pendingActionExecutionId())));
    }

    private boolean matchesPending(StoryExecutionState state, ExternalCommand command) {
        return command.actionExecutionId().asString().equals(state.pendingActionExecutionId());
    }

    private RuntimeTransitionPolicy policy(CompiledStoryCatalogEntry entry) {
        return new RuntimeTransitionPolicy(entry.definition(), entry.actionTypes(), TRANSITION_BUDGET);
    }

    public boolean terminal(StoryExecutionState state) {
        return state.phase() == StoryExecutionPhase.FINISHED || state.phase() == StoryExecutionPhase.FAILED;
    }

    private record LoadedCheckpoint(ExecutionCheckpoint checkpoint, int expectedRevision) {
    }

    private record IncompatibleLoad(
            ExecutionCheckpoint checkpoint,
            ExecutionLoadResult loaded,
            Instant occurredAt) {
    }
}
