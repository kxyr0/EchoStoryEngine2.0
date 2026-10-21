package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.EchoSaveManifest;
import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.api.StateValue;
import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import io.github.kxyr0.echosavemigration.graph.MigrationGraph;
import io.github.kxyr0.echosavemigration.persistence.codec.BinarySaveCodec;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalState;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalStore;
import io.github.kxyr0.echosavemigration.transaction.error.MigrationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationCoordinatorTest {
    @TempDir Path directory;
    private final BinarySaveCodec codec = new BinarySaveCodec();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void migratesFourteenToFifteenWithBackupAndAtomicCommit() throws Exception {
        Path source = source();
        MigrationCoordinator coordinator = coordinator(MigrationFailureHook.none(), valid());

        MigrationReport report = coordinator.migrate(request(source, false));
        SaveSnapshot migrated = codec.read(source);

        assertEquals(MigrationReport.Status.COMMITTED, report.status());
        assertEquals(15, migrated.manifest().schema().value());
        assertEquals(new ResourceId("jdh:university_arrival"), migrated.resourceReferences().get("currentStory"));
        assertEquals(new NodeId("morning_start"),
                migrated.dialogueNodes().get(new ResourceId("jdh:morning_dialogue")));
        assertEquals(new StateValue.IntState(4),
                migrated.states().get(new ResourceId("jdh:relationship.sasha.trust")));
        assertTrue(Files.exists(report.backup().orElseThrow()));
    }

    @Test
    void recoversCrashAfterCandidateWriteWithoutDoubleMigration() throws Exception {
        Path source = source();
        MigrationFailureHook crash = state -> {
            if (state == MigrationJournalState.CANDIDATE_WRITTEN) throw new IllegalStateException("simulated crash");
        };
        assertThrows(MigrationException.class, () -> coordinator(crash, valid()).migrate(request(source, false)));
        assertEquals(14, codec.read(source).manifest().schema().value());

        var recovered = coordinator(MigrationFailureHook.none(), valid())
                .recover(directory.resolve("migration.journal")).orElseThrow();

        assertEquals(MigrationReport.Status.RECOVERED, recovered.status());
        assertEquals(15, codec.read(source).manifest().schema().value());
        assertEquals(1, codec.read(source).receipts().size());
    }

    @Test
    void invalidCandidateLeavesOriginalUntouchedAndKeepsBackup() throws Exception {
        Path source = source();
        byte[] original = Files.readAllBytes(source);
        CandidateValidator invalid = (candidate, expected) -> CandidateValidator.ValidationResult.refused("bad ref");

        MigrationException failure = assertThrows(MigrationException.class,
                () -> coordinator(MigrationFailureHook.none(), invalid).migrate(request(source, false)));

        assertEquals("SAVE_MIGRATION_VALIDATION_FAILED", failure.code());
        assertTrue(java.util.Arrays.equals(original, Files.readAllBytes(source)));
        try (var backups = Files.list(directory.resolve("backups"))) {
            assertFalse(backups.toList().isEmpty());
        }
    }

    private MigrationCoordinator coordinator(MigrationFailureHook hook, CandidateValidator validator) {
        MigrationDefinition step = new MigrationDefinition(new MigrationId("jdh:save_14_to_15"), version(14),
                version(15), 1, List.of(
                new MigrationOperation.RenameResource(resource("jdh:first_university"),
                        resource("jdh:university_arrival")),
                new MigrationOperation.RenameDialogueNode(resource("jdh:morning_dialogue"),
                        new NodeId("old_start"), new NodeId("morning_start")),
                new MigrationOperation.MoveState(resource("jdh:trust_sasha"),
                        resource("jdh:relationship.sasha.trust"))));
        return new MigrationCoordinator(new MigrationGraph(List.of(step)), codec, validator,
                new MigrationJournalStore(), clock, hook);
    }

    private CandidateValidator valid() {
        return (candidate, expected) -> CandidateValidator.ValidationResult.accepted();
    }

    private Path source() throws Exception {
        Path source = directory.resolve("echo-save.dat");
        SaveSnapshot snapshot = new SaveSnapshot(new EchoSaveManifest(version(14), 0, "content-a", Map.of()),
                Map.of("currentStory", resource("jdh:first_university")),
                Map.of(resource("jdh:morning_dialogue"), new NodeId("old_start")), Map.of(),
                Map.of(resource("jdh:trust_sasha"), new StateValue.IntState(4)), List.of());
        codec.write(source, snapshot);
        return source;
    }

    private MigrationRequest request(Path source, boolean dryRun) {
        return new MigrationRequest(source, directory.resolve("backups"), directory.resolve("migration.journal"),
                version(15), dryRun);
    }

    private SaveSchemaVersion version(int value) { return new SaveSchemaVersion(value); }
    private ResourceId resource(String value) { return new ResourceId(value); }
}
