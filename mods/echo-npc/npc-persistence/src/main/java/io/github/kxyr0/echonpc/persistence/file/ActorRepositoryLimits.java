package io.github.kxyr0.echonpc.persistence.file;

/**
 * Conservative bounds for one world actor repository.
 */
public record ActorRepositoryLimits(int maxActorRecords) {
    public ActorRepositoryLimits {
        if (maxActorRecords < 1 || maxActorRecords > 10_000) {
            throw new IllegalArgumentException("maxActorRecords must be within [1, 10000].");
        }
    }

    public static ActorRepositoryLimits standard() {
        return new ActorRepositoryLimits(2048);
    }
}
