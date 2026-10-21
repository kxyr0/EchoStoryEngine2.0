package io.github.kxyr0.echoscene.persistence;

import static org.junit.jupiter.api.Assertions.*;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SceneFileStoreTest {
    @TempDir Path root;
    @Test void keepsPinnedDefinitionExecutionAudienceAndCompletionAfterRestart() {
        var store = new SceneFileStore(root);
        var definition = new SceneDefinition(new SceneResourceId("jdh:father_leaves"), new SceneLocalId("Start"),
                new SceneRevision(7), List.of());
        var checkpoint = new SceneCheckpoint(definition.resourceId(), definition.entry(), definition.revision(),
                new SceneExecutionId("event-reaction-582:action:0"), "SceneExecution#51", SceneExecutionState.COMPLETED,
                0, Set.of(new SceneStepId("step_0_0")), Set.of(), List.of(), Optional.empty());
        var saved = new SavedScene(definition, checkpoint, UUID.randomUUID(), 194, Optional.of("outbox-pinned-before-commit"));
        store.save(saved);
        var restarted = new SceneFileStore(root);
        assertEquals(saved, restarted.load(checkpoint.executionId()).orElseThrow());
        assertEquals(List.of(saved), restarted.loadAll());
    }
    @Test void unfinishedTemporaryWriteDoesNotReplaceCommittedSnapshot() throws Exception {
        var store = new SceneFileStore(root);
        Files.createDirectories(root);
        Files.writeString(root.resolve("uncommitted.scene-state.writing"), "truncated");
        assertTrue(store.loadAll().isEmpty());
    }
}
