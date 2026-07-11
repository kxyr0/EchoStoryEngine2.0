package io.github.kxyr0.echostoryengine.story.core.application;

import io.github.kxyr0.echostoryengine.story.api.StoryEngineApi;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.util.Objects;

/**
 * Application use case that coordinates loading state, applying a pure transition, saving state, and diagnostics.
 */
public final class StoryRuntimeUseCase implements StoryEngineApi {
    private final ExecutionStateRepository stateRepository;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final RuntimeTransitionPolicy transitionPolicy;

    public StoryRuntimeUseCase(
            ExecutionStateRepository stateRepository,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            RuntimeTransitionPolicy transitionPolicy) {
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.transitionPolicy = Objects.requireNonNull(transitionPolicy, "transitionPolicy");
    }

    @Override
    public StoryRuntimeResult advance(StoryRuntimeInput input) {
        Objects.requireNonNull(input, "input");
        StoryExecutionState current = stateRepository
                .load(input.executionId())
                .orElseGet(() -> StoryExecutionState.start(input.storyId(), input.executionId()));
        StoryRuntimeResult result = transitionPolicy.advance(current, input, clock.now());
        stateRepository.save(result.state());
        result.diagnostics().forEach(diagnosticSink::accept);
        return result;
    }
}
