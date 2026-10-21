package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Comparator;

final class BackupService {
    private final Clock clock;
    private final int retention;

    BackupService(Clock clock, int retention) {
        this.clock = clock;
        this.retention = retention;
    }

    Path create(Path source, Path root, SaveSchemaVersion schema) throws IOException {
        Files.createDirectories(root);
        String name = source.getFileName() + "." + clock.millis() + ".schema" + schema.value() + ".backup";
        Path backup = root.resolve(name).toAbsolutePath().normalize();
        if (!backup.startsWith(root.toAbsolutePath().normalize())) throw new IOException("Unsafe backup path.");
        Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
        prune(root);
        return backup;
    }

    private void prune(Path root) throws IOException {
        try (var files = Files.list(root)) {
            var ordered = files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong(this::modified).reversed()).toList();
            for (int index = retention; index < ordered.size(); index++) Files.deleteIfExists(ordered.get(index));
        }
    }

    private long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException failure) { return Long.MIN_VALUE; }
    }
}
