package io.github.kxyr0.echodialogue.core.persistence;

/**
 * Optimistic durable repository revision.
 */
public record DialogueRepositoryRevision(long value) {
    public DialogueRepositoryRevision {
        if (value < 0) {
            throw new IllegalArgumentException("Repository revision must not be negative.");
        }
    }

    public DialogueRepositoryRevision next() {
        return new DialogueRepositoryRevision(value + 1);
    }
}
