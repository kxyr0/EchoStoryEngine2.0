package io.github.kxyr0.echosavemigration.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echosavemigration.persistence.codec.BinarySaveCodec;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalStore;
import io.github.kxyr0.echosavemigration.transaction.CandidateValidator;
import io.github.kxyr0.echosavemigration.transaction.MigrationCoordinator;
import io.github.kxyr0.echosavemigration.transaction.MigrationFailureHook;
import io.github.kxyr0.echosavemigration.transaction.MigrationReport;
import io.github.kxyr0.echosavemigration.transaction.MigrationRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

final class ForgeMigrationRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ForgeMigrationCatalog catalog;

    ForgeMigrationRuntime(ForgeMigrationCatalog catalog) { this.catalog = catalog; }

    void migrateOnStart(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("echo");
        Path source = root.resolve("save.bin");
        if (!Files.isRegularFile(source)) return;
        Path journal = root.resolve("migration.journal");
        MigrationCoordinator coordinator = coordinator();
        coordinator.recover(journal).ifPresent(this::report);
        MigrationReport result = coordinator.migrate(new MigrationRequest(source, root.resolve("backups"), journal,
                catalog.currentSchema(), false));
        report(result);
    }

    private MigrationCoordinator coordinator() {
        CandidateValidator validator = (candidate, expected) -> candidate.manifest().schema().equals(expected)
                ? CandidateValidator.ValidationResult.accepted()
                : CandidateValidator.ValidationResult.refused("Candidate schema does not match runtime schema.");
        return new MigrationCoordinator(catalog.graph(), new BinarySaveCodec(), validator,
                new MigrationJournalStore(), Clock.systemUTC(), MigrationFailureHook.none());
    }

    private void report(MigrationReport report) {
        LOGGER.info("Echo save migration status={} from={} to={} migrations={} operations={} backup={}",
                report.status(), report.from().value(), report.to().value(), report.migrations(),
                report.operations(), report.backup().map(Path::toString).orElse("none"));
    }
}
