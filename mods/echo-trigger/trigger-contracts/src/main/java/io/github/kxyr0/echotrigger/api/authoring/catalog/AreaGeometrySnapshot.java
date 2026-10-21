package io.github.kxyr0.echotrigger.api.authoring.catalog;

import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public record AreaGeometrySnapshot(
        AreaShapeKind shapeKind,
        Vector3 min,
        Vector3 max,
        Vector3 center,
        OptionalDouble radius,
        List<Vector3> polygonPoints
) {
    public AreaGeometrySnapshot {
        shapeKind = Objects.requireNonNull(shapeKind, "shapeKind");
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        center = Objects.requireNonNull(center, "center");
        radius = Objects.requireNonNull(radius, "radius");
        polygonPoints = List.copyOf(Objects.requireNonNull(polygonPoints, "polygonPoints"));
    }
}
