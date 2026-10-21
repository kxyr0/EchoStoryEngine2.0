package io.github.kxyr0.echodebug.core.trail;

import java.util.List;
import java.util.Objects;

public record NpcMovementTrail(
        String moveId,
        String subjectId,
        String dimension,
        List<NpcTrailPoint> points,
        long firstServerTick,
        long lastServerTick,
        NpcTrailStatus status,
        long trailRevision
) {
    public NpcMovementTrail {
        moveId = Objects.requireNonNull(moveId, "moveId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        dimension = Objects.requireNonNull(dimension, "dimension");
        points = List.copyOf(points);
        status = Objects.requireNonNull(status, "status");
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Trail must contain at least one point.");
        }
    }

    public boolean active() {
        return !status.terminal();
    }
}
