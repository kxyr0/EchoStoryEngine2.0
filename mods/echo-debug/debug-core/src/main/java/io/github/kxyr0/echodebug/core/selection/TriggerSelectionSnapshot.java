package io.github.kxyr0.echodebug.core.selection;

import io.github.kxyr0.echodebug.core.identity.TriggerWandSessionId;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TriggerSelectionSnapshot(
        TriggerWandSessionId sessionId,
        String playerId,
        TriggerWandShapeMode shape,
        Optional<String> dimension,
        List<TriggerBlockPoint> points,
        long revision,
        Optional<TriggerSelectionGeometry> geometry
) {
    public TriggerSelectionSnapshot {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        playerId = Objects.requireNonNull(playerId, "playerId").trim();
        shape = Objects.requireNonNull(shape, "shape");
        dimension = Objects.requireNonNull(dimension, "dimension");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        geometry = Objects.requireNonNull(geometry, "geometry");
        if (playerId.isEmpty() || revision < 0) {
            throw new IllegalArgumentException("Snapshot player and revision are required.");
        }
    }

    public boolean complete() {
        return shape == TriggerWandShapeMode.SEAT ? points.size() == 1 && dimension.isPresent()
                : geometry.isPresent();
    }

    public int requiredPoints() {
        return shape.minimumPoints();
    }
}
