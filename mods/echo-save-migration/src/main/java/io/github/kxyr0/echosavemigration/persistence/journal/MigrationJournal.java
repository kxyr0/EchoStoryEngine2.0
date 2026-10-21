package io.github.kxyr0.echosavemigration.persistence.journal;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MigrationJournal(
        MigrationJournalState state,
        Path source,
        Path candidate,
        Path backup,
        SaveSchemaVersion from,
        SaveSchemaVersion to,
        List<MigrationId> migrations,
        String candidateHash,
        Instant updatedAt
) {
    public MigrationJournal {
        state = Objects.requireNonNull(state, "state");
        source = source.toAbsolutePath().normalize();
        candidate = candidate.toAbsolutePath().normalize();
        backup = backup.toAbsolutePath().normalize();
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        migrations = List.copyOf(migrations);
        candidateHash = Objects.requireNonNull(candidateHash, "candidateHash");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public MigrationJournal transition(MigrationJournalState next, String hash) {
        if (next.ordinal() < state.ordinal()) throw new IllegalArgumentException("Journal transition moved backwards.");
        return new MigrationJournal(next, source, candidate, backup, from, to, migrations, hash, Instant.now());
    }
}
