package io.github.kxyr0.echosavemigration.compile;

import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveMigrationCompilerTest {
    @Test
    void compilesCanonicalPureMigrationDsl() {
        MigrationCompileResult result = new SaveMigrationCompiler().compile(new MigrationSourceInput(
                "jdh", "save_14_to_15.migration.es", """
                class Save14To15 : SaveMigrationResource
                {
                    From(14);
                    To(15);
                    RenameResource("jdh:first_university", "jdh:university_arrival");
                    RenameDialogueNode("jdh:morning_dialogue", "old_start", "morning_start");
                    MoveState("jdh:trust_sasha", "jdh:relationship.sasha.trust");
                }
                """, 3));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(14, result.definition().orElseThrow().from().value());
        assertEquals(15, result.definition().orElseThrow().to().value());
        assertEquals(3, result.definition().orElseThrow().operations().size());
        assertInstanceOf(MigrationOperation.RenameResource.class,
                result.definition().orElseThrow().operations().get(0));
    }

    @Test
    void rejectsWorldCommands() {
        MigrationCompileResult result = new SaveMigrationCompiler().compile(new MigrationSourceInput(
                "jdh", "bad.es", "class Bad : SaveMigrationResource { From(14); To(15); Npc.Move(); }", 1));

        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("SAVE_MIGRATION_STEP_FAILED")),
                () -> result.diagnostics().toString());
    }
}
