package io.github.kxyr0.echosavemigration.persistence.journal;

public enum MigrationJournalState {
    PLANNED,
    BACKED_UP,
    APPLYING,
    CANDIDATE_WRITTEN,
    VERIFIED,
    COMMITTED
}
