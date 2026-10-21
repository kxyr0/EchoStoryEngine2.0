package io.github.kxyr0.echostoryengine.story.persistence;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class PersistenceTestFixture {
    public static final Instant CREATED = Instant.parse("2026-07-12T00:00:00Z");
    public static final Instant UPDATED = Instant.parse("2026-07-12T00:01:00Z");
    public static final StoryId STORY_ID = StoryId.of("test:persistence_story");
    public static final ExecutionId EXECUTION_ID = ExecutionId.of("test:execution");
    public static final NamespacedId ACTION_TYPE = NamespacedId.of("test:external_action");
    public static final ActionExecutionId ACTION_ID = ActionExecutionId.of("test:execution/request");
    public static final NodeId START = NodeId.root();
    public static final NodeId REQUEST = NodeId.of("request");
    public static final NodeId FINISH = NodeId.of("finish");

    private PersistenceTestFixture() {
    }

    public static StoryDefinition story() {
        return new StoryDefinition(
                STORY_ID,
                START,
                Map.of(
                        START, StoryNode.start(START, REQUEST),
                        REQUEST, StoryNode.externalAction(REQUEST, ACTION_TYPE, arguments(), FINISH),
                        FINISH, StoryNode.finish(FINISH)));
    }

    public static StoryDefinition storyWithoutCurrentNode() {
        return new StoryDefinition(STORY_ID, START, Map.of(START, StoryNode.finish(START)));
    }

    public static ExecutionCheckpoint waitingCheckpoint() {
        StoryExecutionState state = new StoryExecutionState(
                STORY_ID,
                EXECUTION_ID,
                REQUEST,
                StoryExecutionPhase.WAITING,
                Map.of("hello.flag", "ready"),
                ACTION_ID.asString(),
                "",
                "",
                3);
        PendingActionCheckpoint pending = new PendingActionCheckpoint(ACTION_ID, ACTION_TYPE, arguments());
        return new ExecutionCheckpoint(state, fingerprint(), Optional.of(pending));
    }

    public static ExecutionCheckpoint readyCheckpoint() {
        return ExecutionCheckpoint.withoutPending(
                StoryExecutionState.start(STORY_ID, EXECUTION_ID),
                fingerprint());
    }

    public static ExecutionCheckpoint finishedCheckpoint() {
        StoryExecutionState state = waitingCheckpoint().state()
                .completedAction(ACTION_ID, FINISH)
                .finishedAt(FINISH);
        return ExecutionCheckpoint.withoutPending(state, fingerprint());
    }

    public static ExecutionCheckpoint runningCheckpoint() {
        StoryExecutionState state = StoryExecutionState.start(STORY_ID, EXECUTION_ID).runningAt(REQUEST);
        return ExecutionCheckpoint.withoutPending(state, fingerprint());
    }

    public static Map<String, String> arguments() {
        return Map.of("target", "abstract", "mode", "recoverable");
    }

    public static CompiledStoryFingerprint fingerprint() {
        return StoryDefinitionFingerprint.from(story());
    }

    public static Path primaryFile(Path root) throws IOException {
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(path -> path.toString().endsWith(".json")).findFirst().orElseThrow();
        }
    }

    public static Path backupFile(Path root) throws IOException {
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(path -> path.toString().endsWith(".bak")).findFirst().orElseThrow();
        }
    }

    public static final class MutableClock implements StoryClock {
        private Instant now = CREATED;

        @Override
        public Instant now() {
            return now;
        }

        public void set(Instant now) {
            this.now = now;
        }
    }
}
