package io.github.kxyr0.echoevents.core.journal;

public interface EventJournalStore {
    EventJournalImage load();
    /** Atomic, durable replacement; a failure must leave the previous image readable. */
    void commit(EventJournalImage image);
}
