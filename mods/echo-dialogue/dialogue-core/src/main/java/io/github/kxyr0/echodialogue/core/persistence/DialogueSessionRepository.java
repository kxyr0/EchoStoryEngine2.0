package io.github.kxyr0.echodialogue.core.persistence;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Core-owned persistence port without filesystem or codec concepts.
 */
public interface DialogueSessionRepository {
    DialogueRepositoryLoadResult load(DialogueSessionId sessionId);

    List<DialogueRepositoryLoadResult> loadAll();

    DialogueRepositoryWriteResult save(DialogueCheckpoint checkpoint, SaveExpectation expectation);

    DialogueRepositoryWriteResult acknowledgeTerminal(
            DialogueSessionId sessionId,
            DialogueRepositoryRevision expectedRevision
    );

    DialogueRepositoryWriteResult cleanupTerminal(long nowEpochMillis, long maxAgeMillis, int maxRecords);

    record SaveExpectation(Optional<DialogueRepositoryRevision> revision) {
        public SaveExpectation {
            revision = Objects.requireNonNull(revision, "revision");
        }

        public static SaveExpectation expectedEmpty() {
            return new SaveExpectation(Optional.empty());
        }

        public static SaveExpectation expectedRevision(DialogueRepositoryRevision revision) {
            return new SaveExpectation(Optional.of(revision));
        }
    }
}
