package io.github.kxyr0.echonpc.api.service;

/**
 * Bounded public service health view.
 */
public record NpcServiceHealth(
        boolean active,
        long catalogRevision,
        int definitionCount,
        int activeActorCount,
        int durableActorCount,
        int blockedActorCount,
        int queuedOperationCount,
        boolean degraded,
        boolean hasWarnings,
        boolean hasErrors
) {
    public NpcServiceHealth {
        if (catalogRevision < 0L || definitionCount < 0 || activeActorCount < 0
                || durableActorCount < 0 || blockedActorCount < 0 || queuedOperationCount < 0) {
            throw new IllegalArgumentException("Health counters must not be negative.");
        }
    }

    public NpcServiceHealth(
            boolean active,
            long catalogRevision,
            int definitionCount,
            int activeActorCount,
            boolean hasWarnings,
            boolean hasErrors
    ) {
        this(active, catalogRevision, definitionCount, activeActorCount, activeActorCount, 0, 0, false,
                hasWarnings, hasErrors);
    }
}
