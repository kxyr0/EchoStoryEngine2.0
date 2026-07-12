package io.github.kxyr0.echostoryengine.story.core.application;

import io.github.kxyr0.echostoryengine.story.api.StoryEngineApi;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionSaveStatus;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Application use case that coordinates loading state, applying a pure transition, saving state, and diagnostics.
 */
public final class StoryRuntimeUseCase implements StoryEngineApi {
    private final ExecutionStateRepository stateRepository;
    private final StoryClock clock;
    private final RuntimeTransitionPolicy transitionPolicy;
    private final RuntimePersistenceResults results;

    public StoryRuntimeUseCase(
            ExecutionStateRepository stateRepository,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            RuntimeTransitionPolicy transitionPolicy) {
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.transitionPolicy = Objects.requireNonNull(transitionPolicy, "transitionPolicy");
        this.results = new RuntimePersistenceResults(Objects.requireNonNull(diagnosticSink, "diagnosticSink"));
    }

    @Override
    public StoryRuntimeResult advance(StoryRuntimeInput input) {
        Objects.requireNonNull(input, "input");
        Instant occurredAt = clock.now();
        ExecutionLoadResult loaded = stateRepository.load(input.executionId());
        if (loaded.status() == ExecutionLoadStatus.FAILED) {
            return results.loadFailed(input, loaded, occurredAt);
        }
        LoadedCheckpoint current = currentCheckpoint(input, loaded);
        if (!isCompatible(current.checkpoint())) {
            return results.incompatible(input, current.checkpoint(), loaded.diagnostics(), occurredAt);
        }
        StoryRuntimeResult transition = transitionPolicy.advance(current.checkpoint().state(), input, occurredAt);
        ExecutionCheckpoint nextCheckpoint = nextCheckpoint(current.checkpoint(), transition);
        ExecutionSaveResult saved = stateRepository.save(nextCheckpoint, current.expectedRevision());
        if (saved.status() == ExecutionSaveStatus.SAVED) {
            return results.success(input, transition, loaded.diagnostics(), saved.diagnostics());
        }
        RuntimePersistenceResults.SaveFailure failure = new RuntimePersistenceResults.SaveFailure(
                input,
                current.checkpoint().state(),
                transition,
                saved,
                loaded.diagnostics(),
                occurredAt);
        return results.saveSuppressed(failure);
    }

    private LoadedCheckpoint currentCheckpoint(StoryRuntimeInput input, ExecutionLoadResult loaded) {
        if (loaded.status() == ExecutionLoadStatus.FOUND) {
            ExecutionCheckpoint checkpoint = loaded.checkpoint().orElseThrow();
            return new LoadedCheckpoint(checkpoint, checkpoint.state().revision());
        }
        StoryExecutionState state = StoryExecutionState.start(input.storyId(), input.executionId());
        ExecutionCheckpoint checkpoint = ExecutionCheckpoint.withoutPending(
                state,
                transitionPolicy.definitionFingerprint());
        return new LoadedCheckpoint(checkpoint, -1);
    }

    private boolean isCompatible(ExecutionCheckpoint checkpoint) {
        return checkpoint.definitionFingerprint().equals(transitionPolicy.definitionFingerprint());
    }

    private ExecutionCheckpoint nextCheckpoint(ExecutionCheckpoint current, StoryRuntimeResult transition) {
        return new ExecutionCheckpoint(
                transition.state(),
                transitionPolicy.definitionFingerprint(),
                pendingAction(current, transition));
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

    private record LoadedCheckpoint(ExecutionCheckpoint checkpoint, int expectedRevision) {
    }
}
