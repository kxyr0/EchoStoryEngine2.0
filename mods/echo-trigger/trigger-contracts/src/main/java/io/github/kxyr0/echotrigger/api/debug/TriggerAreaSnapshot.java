package io.github.kxyr0.echotrigger.api.debug;

import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.Objects;

public record TriggerAreaSnapshot(
        TriggerId triggerId,
        String areaName,
        String dimension,
        AreaShapeKind shapeKind,
        Vector3 min,
        Vector3 max,
        Vector3 center
) {
    public TriggerAreaSnapshot {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        areaName = Objects.requireNonNull(areaName, "areaName").trim();
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        shapeKind = Objects.requireNonNull(shapeKind, "shapeKind");
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        center = Objects.requireNonNull(center, "center");
        if (areaName.isEmpty() || dimension.isEmpty()) {
            throw new IllegalArgumentException("Area snapshot requires areaName and dimension.");
        }
    }
}
