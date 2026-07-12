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
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RuntimeTestFixture {
    static final Instant FIXED_TIME = Instant.parse("2026-07-11T00:00:00Z");
    static final StoryId STORY_ID = StoryId.of("echo_story_engine:hello_story");
    static final ExecutionId EXECUTION_ID = ExecutionId.of("echo_story_engine:test-execution");
    static final NamespacedId ACTION_TYPE = NamespacedId.of("echo_story_engine:hello_action");
    static final NodeId ACTION_NODE = NodeId.of("request_hello_action");
    static final ActionExecutionId ACTION_ID = actionId(EXECUTION_ID);
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
                        ACTION_NODE, StoryNode.externalAction(ACTION_NODE, ACTION_TYPE, Map.of(), FINISH_NODE),
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
        return completion(executionId, actionId(executionId), ExternalActionResultCategory.SUCCESS);
    }

    static ActionExecutionId actionId(ExecutionId executionId) {
        return ActionExecutionId.of(executionId.value().namespace()
                + ":"
                + executionId.value().path()
                + "/"
                + ACTION_NODE.value());
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
        private final Map<ExecutionId, ExecutionCheckpoint> checkpoints = new HashMap<>();
        private ExecutionSaveResult nextSaveResult;

        @Override
        public ExecutionLoadResult load(ExecutionId executionId) {
            ExecutionCheckpoint checkpoint = checkpoints.get(executionId);
            if (checkpoint == null) {
                return ExecutionLoadResult.notFound(List.of());
            }
            return ExecutionLoadResult.found(checkpoint, List.of());
        }

        @Override
        public ExecutionSaveResult save(ExecutionCheckpoint checkpoint, int expectedRevision) {
            if (nextSaveResult != null) {
                ExecutionSaveResult result = nextSaveResult;
                nextSaveResult = null;
                return result;
            }
            ExecutionCheckpoint current = checkpoints.get(checkpoint.state().executionId());
            if (!matchesRevision(current, expectedRevision)) {
                return ExecutionSaveResult.revisionConflict(List.of());
            }
            checkpoints.put(checkpoint.state().executionId(), checkpoint);
            return ExecutionSaveResult.saved(List.of());
        }

        StoryExecutionState state(ExecutionId executionId) {
            return checkpoints.get(executionId).state();
        }

        ExecutionCheckpoint checkpoint(ExecutionId executionId) {
            return checkpoints.get(executionId);
        }

        void failNextSave(StoryDiagnostic diagnostic) {
            nextSaveResult = ExecutionSaveResult.failed(List.of(diagnostic));
        }

        void conflictNextSave() {
            nextSaveResult = ExecutionSaveResult.revisionConflict(List.of());
        }

        private boolean matchesRevision(ExecutionCheckpoint current, int expectedRevision) {
            if (expectedRevision == -1) {
                return current == null;
            }
            return current != null && current.state().revision() == expectedRevision;
        }
    }
}
