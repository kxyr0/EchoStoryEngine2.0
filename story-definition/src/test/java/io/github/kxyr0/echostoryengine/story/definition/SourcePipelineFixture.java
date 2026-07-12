package io.github.kxyr0.echostoryengine.story.definition;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SourcePipelineFixture {
    public static final Instant TIME = Instant.parse("2026-07-12T00:00:00Z");
    public static final String SOURCE_ID = "test:valid-story.es";
    public static final StoryId STORY_ID = StoryId.of("sample:hello_story");
    public static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:execution");
    public static final NodeId CONFIRM_NODE = NodeId.of("confirm");
    public static final NamespacedId ACTION_TYPE = NamespacedId.of("sample:confirm");
    public static final ActionExecutionId ACTION_ID = ActionExecutionId.of("sample:execution/confirm");

    private SourcePipelineFixture() {
    }

    public static StorySourceInput validInput() {
        return new StorySourceInput(SOURCE_ID, "");
    }

    public static SourceStory validSource() {
        return source(Optional.of(1), Optional.of(STORY_ID.asString()), Optional.of("start"), validNodes());
    }

    public static SourceStory source(
            Optional<Integer> formatVersion,
            Optional<String> storyId,
            Optional<String> startNode,
            List<SourceNode> nodes) {
        return new SourceStory(
                SOURCE_ID,
                formatVersion,
                storyId,
                startNode,
                nodes,
                Set.of("formatVersion", "storyId", "startNode", "nodes"),
                SourceLocation.unknown());
    }

    public static List<SourceNode> validNodes() {
        return List.of(
                node("start", "start", Optional.of("set_ready")),
                setNode("set_ready", "sample.ready", "true", "check_ready"),
                conditionNode("check_ready", "sample.ready", "true", "confirm"),
                actionNode("confirm", "sample:confirm", "finish", Map.of()),
                node("finish", "finish", Optional.empty()));
    }

    public static SourceNode node(String id, String kind, Optional<String> next) {
        Set<String> fields = next.isPresent() ? Set.of("id", "kind", "next") : Set.of("id", "kind");
        return new SourceNode(
                Optional.of(id),
                Optional.of(kind),
                next,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                fields,
                SourceLocation.unknown());
    }

    public static SourceNode setNode(String id, String variable, String value, String next) {
        return new SourceNode(
                Optional.of(id),
                Optional.of("set_variable"),
                Optional.of(next),
                Optional.of(variable),
                Optional.of(value),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "value", "next"),
                SourceLocation.unknown());
    }

    public static SourceNode conditionNode(String id, String variable, String expected, String next) {
        return new SourceNode(
                Optional.of(id),
                Optional.of("condition"),
                Optional.of(next),
                Optional.of(variable),
                Optional.empty(),
                Optional.of(expected),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "expectedValue", "next"),
                SourceLocation.unknown());
    }

    public static SourceNode actionNode(String id, String actionType, String next, Map<String, String> arguments) {
        Set<String> fields = arguments.isEmpty()
                ? Set.of("id", "kind", "actionType", "next")
                : Set.of("id", "kind", "actionType", "arguments", "next");
        return new SourceNode(
                Optional.of(id),
                Optional.of("external_action"),
                Optional.of(next),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(actionType),
                arguments,
                List.of(),
                fields,
                SourceLocation.unknown());
    }

    public static StoryRuntimeInput startInput() {
        return StoryRuntimeInput.start(STORY_ID, EXECUTION_ID);
    }

    public static StoryRuntimeInput successInput() {
        return StoryRuntimeInput.externalActionCompleted(
                STORY_ID,
                EXECUTION_ID,
                ACTION_ID,
                ExternalActionResultCategory.SUCCESS);
    }

    public static StoryRuntimeUseCase useCase(StoryDefinition definition, RecordingRepository repository) {
        RuntimeTransitionPolicy policy = new RuntimeTransitionPolicy(definition, Set.of(ACTION_TYPE), 16);
        return new StoryRuntimeUseCase(repository, new RecordingSink(), new FixedClock(), policy);
    }

    private static final class FixedClock implements StoryClock {
        @Override
        public Instant now() {
            return TIME;
        }
    }

    public static final class RecordingSink implements DiagnosticSink {
        private final List<StoryDiagnostic> diagnostics = new ArrayList<>();

        @Override
        public void accept(StoryDiagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }
    }

    public static final class RecordingRepository implements ExecutionStateRepository {
        private final Map<ExecutionId, ExecutionCheckpoint> checkpoints = new HashMap<>();

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
            ExecutionCheckpoint current = checkpoints.get(checkpoint.state().executionId());
            if (!matchesRevision(current, expectedRevision)) {
                return ExecutionSaveResult.revisionConflict(List.of());
            }
            checkpoints.put(checkpoint.state().executionId(), checkpoint);
            return ExecutionSaveResult.saved(List.of());
        }

        public StoryExecutionState state(ExecutionId executionId) {
            return checkpoints.get(executionId).state();
        }

        private boolean matchesRevision(ExecutionCheckpoint current, int expectedRevision) {
            if (expectedRevision == -1) {
                return current == null;
            }
            return current != null && current.state().revision() == expectedRevision;
        }
    }
}
