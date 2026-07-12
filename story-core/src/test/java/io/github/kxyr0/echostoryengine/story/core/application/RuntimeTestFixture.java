package io.github.kxyr0.echostoryengine.story.core.application;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class RuntimeTestFixture {
    static final Instant FIXED_TIME = Instant.parse("2026-07-11T00:00:00Z");
    static final StoryId STORY_ID = StoryId.of("echo_story_engine:hello_story");
    static final ExecutionId EXECUTION_ID = ExecutionId.of("echo_story_engine:test-execution");
    static final NamespacedId ACTION_TYPE = NamespacedId.of("echo_story_engine:hello_action");
    static final ActionExecutionId ACTION_ID = ActionExecutionId.of("echo_story_engine:hello-action-1");
    static final NodeId ACTION_NODE = NodeId.of("request_hello_action");
    static final NodeId FINISH_NODE = NodeId.of("finished");

    private RuntimeTestFixture() {
    }

    static StoryDefinition helloStory() {
        NodeId start = NodeId.root();
        NodeId setVariable = NodeId.of("set_hello_flag");
        NodeId condition = NodeId.of("check_hello_flag");
        return new StoryDefinition(
                STORY_ID,
                start,
                Map.of(
                        start, StoryNode.start(start, setVariable),
                        setVariable, StoryNode.setVariable(setVariable, "hello.flag", "ready", condition),
                        condition, StoryNode.condition(condition, "hello.flag", "ready", ACTION_NODE),
                        ACTION_NODE, StoryNode.externalAction(ACTION_NODE, ACTION_TYPE, ACTION_ID, FINISH_NODE),
                        FINISH_NODE, StoryNode.finish(FINISH_NODE)));
    }

    static StoryDefinition loopingStory() {
        NodeId start = NodeId.root();
        return new StoryDefinition(STORY_ID, start, Map.of(start, StoryNode.start(start, start)));
    }

    static StoryRuntimeUseCase useCase(RecordingRepository repository, RecordingSink sink) {
        return useCase(repository, sink, helloStory(), Set.of(ACTION_TYPE));
    }

    static StoryRuntimeUseCase useCase(
            RecordingRepository repository,
            RecordingSink sink,
            StoryDefinition definition,
            Set<NamespacedId> supportedActions) {
        RuntimeTransitionPolicy policy = new RuntimeTransitionPolicy(definition, supportedActions, 16);
        return new StoryRuntimeUseCase(repository, sink, new FixedClock(), policy);
    }

    static StoryRuntimeUseCase useCaseWithBudget(StoryDefinition definition, int budget) {
        RuntimeTransitionPolicy policy = new RuntimeTransitionPolicy(definition, Set.of(ACTION_TYPE), budget);
        return new StoryRuntimeUseCase(new RecordingRepository(), new RecordingSink(), new FixedClock(), policy);
    }

    static StoryRuntimeInput startInput() {
        return startInput(EXECUTION_ID);
    }

    static StoryRuntimeInput startInput(ExecutionId executionId) {
        return StoryRuntimeInput.start(STORY_ID, executionId);
    }

    static StoryRuntimeInput successfulCompletion() {
        return completion(EXECUTION_ID, ACTION_ID, ExternalActionResultCategory.SUCCESS);
    }

    static StoryRuntimeInput successfulCompletion(ExecutionId executionId) {
        return completion(executionId, ACTION_ID, ExternalActionResultCategory.SUCCESS);
    }

    static StoryRuntimeInput completion(
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            ExternalActionResultCategory resultCategory) {
        return StoryRuntimeInput.externalActionCompleted(STORY_ID, executionId, actionExecutionId, resultCategory);
    }

    private static final class FixedClock implements StoryClock {
        @Override
        public Instant now() {
            return FIXED_TIME;
        }
    }

    static final class RecordingSink implements DiagnosticSink {
        private final List<StoryDiagnostic> diagnostics = new ArrayList<>();

        @Override
        public void accept(StoryDiagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }

        List<StoryDiagnostic> diagnostics() {
            return List.copyOf(diagnostics);
        }
    }

    static final class RecordingRepository implements ExecutionStateRepository {
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
