package io.github.kxyr0.echostoryengine.story.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class StoryRuntimeUseCaseTest {
    private static final Instant FIXED_TIME = Instant.parse("2026-07-11T00:00:00Z");

    @Test
    void createsInitialStateWhenExecutionIsUnknown() {
        RecordingRepository repository = new RecordingRepository();
        RecordingSink sink = new RecordingSink();
        StoryRuntimeUseCase useCase = useCase(repository, sink);

        StoryRuntimeResult result = useCase.advance(input());

        assertEquals(NodeId.of("runtime.accepted"), result.state().currentNode());
        assertEquals(1, result.state().revision());
        assertEquals(result.state(), repository.load(result.executionId()).orElseThrow());
        assertEquals(1, sink.diagnostics().size());
        assertEquals(FIXED_TIME, result.diagnostics().get(0).occurredAt());
    }

    @Test
    void advancesExistingStateWithoutCreatingPlatformCommands() {
        RecordingRepository repository = new RecordingRepository();
        StoryExecutionState existing = StoryExecutionState.start(StoryId.of("echo_story_engine:test"), executionId());
        repository.save(existing);
        StoryRuntimeUseCase useCase = useCase(repository, new RecordingSink());

        StoryRuntimeResult result = useCase.advance(input());

        assertEquals(1, result.state().revision());
        assertEquals(NodeId.of("runtime.accepted"), result.state().currentNode());
        assertFalse(result.commands().stream().anyMatch(command -> command.actionType().path().contains("minecraft")));
    }

    private static StoryRuntimeUseCase useCase(RecordingRepository repository, RecordingSink sink) {
        return new StoryRuntimeUseCase(repository, sink, new FixedClock(), new RuntimeTransitionPolicy());
    }

    private static StoryRuntimeInput input() {
        return StoryRuntimeInput.of(
                StoryId.of("echo_story_engine:test"),
                executionId(),
                NamespacedId.of("echo_story_engine:test_event"),
                Map.of("source", "unit-test"));
    }

    private static ExecutionId executionId() {
        return ExecutionId.of("echo_story_engine:test-execution");
    }

    private static final class FixedClock implements StoryClock {
        @Override
        public Instant now() {
            return FIXED_TIME;
        }
    }

    private static final class RecordingSink implements DiagnosticSink {
        private final List<StoryDiagnostic> diagnostics = new ArrayList<>();

        @Override
        public void accept(StoryDiagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }

        List<StoryDiagnostic> diagnostics() {
            return List.copyOf(diagnostics);
        }
    }

    private static final class RecordingRepository implements ExecutionStateRepository {
        private final Map<ExecutionId, StoryExecutionState> states = new HashMap<>();

        @Override
        public Optional<StoryExecutionState> load(ExecutionId executionId) {
            return Optional.ofNullable(states.get(executionId));
        }

        @Override
        public void save(StoryExecutionState state) {
            states.put(state.executionId(), state);
        }
    }
}
