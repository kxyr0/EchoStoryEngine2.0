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
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.StorySourcePipeline;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SourcePipelineFixture {
    public static final Instant TIME = Instant.parse("2026-07-12T00:00:00Z");
    public static final String SOURCE_ID = "test:valid-story.json";
    public static final StoryId STORY_ID = StoryId.of("sample:hello_story");
    public static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:execution");
    public static final NodeId CONFIRM_NODE = NodeId.of("confirm");
    public static final NamespacedId ACTION_TYPE = NamespacedId.of("sample:confirm");
    public static final ActionExecutionId ACTION_ID = ActionExecutionId.of("sample:execution/confirm");

    private SourcePipelineFixture() {
    }

    public static StorySourceInput validInput() {
        return new StorySourceInput(SOURCE_ID, resourceText("/source/valid-story.json"));
    }

    public static StoryCompilationResult compile(String content) {
        return new StorySourcePipeline().compile(new StorySourceInput(SOURCE_ID, content), TIME);
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

    private static String resourceText(String path) {
        try (var stream = SourcePipelineFixture.class.getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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
