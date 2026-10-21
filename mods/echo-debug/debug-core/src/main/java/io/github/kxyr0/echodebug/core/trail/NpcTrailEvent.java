package io.github.kxyr0.echodebug.core.trail;

import java.util.Objects;

public record NpcTrailEvent(
        String moveId,
        String subjectId,
        String dimension,
        NpcTrailPoint point,
        NpcTrailStatus status,
        long trailRevision
) {
    public NpcTrailEvent {
        moveId = requireText(moveId, "moveId");
        subjectId = requireText(subjectId, "subjectId");
        dimension = requireText(dimension, "dimension");
        point = Objects.requireNonNull(point, "point");
        status = Objects.requireNonNull(status, "status");
        if (trailRevision < 0L) {
            throw new IllegalArgumentException("trailRevision must not be negative.");
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return trimmed;
    }
}
