package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalState;

@FunctionalInterface
public interface MigrationFailureHook {
    void after(MigrationJournalState state);

    static MigrationFailureHook none() {
        return ignored -> { };
    }
}
