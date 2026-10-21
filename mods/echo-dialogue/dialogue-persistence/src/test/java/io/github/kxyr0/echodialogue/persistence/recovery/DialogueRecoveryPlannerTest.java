package io.github.kxyr0.echodialogue.persistence.recovery;

import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.activeCheckpoint;
import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.changedDialogue;
import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.dialogue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DialogueRecoveryPlannerTest {
    private final DialogueRecoveryPlanner planner = new DialogueRecoveryPlanner();

    @Test
    void compatibleFingerprintRestoresActiveAsDetached() {
        var checkpoint = activeCheckpoint(1000);

        DialogueRecoveryPlan plan = planner.plan(checkpoint, Map.of(checkpoint.dialogueId(), dialogue()));

        assertEquals(DialogueRecoveryPlan.Kind.ACTIVE_DETACHED, plan.kind());
        assertEquals(checkpoint.sessionId(), plan.checkpoint().orElseThrow().sessionId());
    }

    @Test
    void missingOrIncompatibleDefinitionBlocksWithoutRestart() {
        var checkpoint = activeCheckpoint(1000);

        DialogueRecoveryPlan missing = planner.plan(checkpoint, Map.of());
        DialogueRecoveryPlan changed = planner.plan(checkpoint, Map.of(checkpoint.dialogueId(), changedDialogue()));

        assertEquals(DialogueRecoveryPlan.Kind.BLOCKED, missing.kind());
        assertEquals(DialogueRecoveryPlan.Kind.BLOCKED, changed.kind());
    }

    @Test
    void formerlyMigratesSavedCurrentNodeWithoutRestartingAtEntry() {
        var checkpoint = activeCheckpoint(1000);
        DialogueNodeId current = new DialogueNodeId("university_entrance");
        CompiledDialogue migrated = CompiledDialogue.linear(checkpoint.dialogueId(), current, List.of(
                new CompiledDialogueLine(current, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?"),
                new CompiledDialogueLine(current, 1, SpeakerId.parse("sample:player"), "Игрок", "Гулял.")));
        migrated = new CompiledDialogue(migrated.id(), migrated.startNodeId(), migrated.nodes(),
                Map.of(checkpoint.currentNodeId(), current));

        DialogueRecoveryPlan plan = planner.plan(checkpoint, Map.of(checkpoint.dialogueId(), migrated));

        assertEquals(DialogueRecoveryPlan.Kind.ACTIVE_DETACHED, plan.kind());
        assertEquals(current, plan.checkpoint().orElseThrow().currentNodeId());
        assertEquals(0, plan.checkpoint().orElseThrow().lineOrdinal());
    }
}
