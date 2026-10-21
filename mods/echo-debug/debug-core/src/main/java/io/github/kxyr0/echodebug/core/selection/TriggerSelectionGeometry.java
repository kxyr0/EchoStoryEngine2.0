package io.github.kxyr0.echodebug.core.selection;

import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public record TriggerSelectionGeometry(
        AreaShapeKind shapeKind,
        Vector3 min,
        Vector3 max,
        Vector3 center,
        OptionalDouble radius,
        List<Vector3> polygonPoints
) {
    public TriggerSelectionGeometry {
        shapeKind = Objects.requireNonNull(shapeKind, "shapeKind");
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        center = Objects.requireNonNull(center, "center");
        radius = Objects.requireNonNull(radius, "radius");
        polygonPoints = List.copyOf(Objects.requireNonNull(polygonPoints, "polygonPoints"));
    }

    public static Optional<TriggerSelectionGeometry> from(TriggerWandShapeMode mode, List<TriggerBlockPoint> points) {
        if (!mode.complete(points.size())) {
            return Optional.empty();
        }
        return switch (mode) {
            case BOX -> Optional.of(box(points.get(0), points.get(1)));
            case SPHERE -> sphere(points.get(0), points.get(1));
            case CYLINDER -> cylinder(points.get(0), points.get(1), points.get(2));
            case POLYGON -> polygon(points);
            case SEAT -> Optional.empty();
        };
    }

    private static TriggerSelectionGeometry box(TriggerBlockPoint first, TriggerBlockPoint second) {
        Vector3 min = min(first.lowerCorner(), second.lowerCorner());
        Vector3 max = max(first.upperExclusiveCorner(), second.upperExclusiveCorner());
        return new TriggerSelectionGeometry(AreaShapeKind.BOX, min, max, min.add(max).divide(2.0D),
                OptionalDouble.empty(), List.of());
    }

    private static Optional<TriggerSelectionGeometry> sphere(TriggerBlockPoint center, TriggerBlockPoint radius) {
        Vector3 c = center.lowerCorner();
        double r = distance(c, radius.lowerCorner());
        if (r <= 0.0D) {
            return Optional.empty();
        }
        return Optional.of(new TriggerSelectionGeometry(AreaShapeKind.SPHERE,
                new Vector3(c.X() - r, c.Y() - r, c.Z() - r),
                new Vector3(c.X() + r, c.Y() + r, c.Z() + r), c, OptionalDouble.of(r), List.of()));
    }

    private static Optional<TriggerSelectionGeometry> cylinder(
            TriggerBlockPoint center,
            TriggerBlockPoint radius,
            TriggerBlockPoint height
    ) {
        Vector3 c = center.lowerCorner();
        double r = Math.hypot(radius.x() - center.x(), radius.z() - center.z());
        if (r <= 0.0D) {
            return Optional.empty();
        }
        double minY = Math.min(center.y(), height.y());
        double maxY = Math.max(center.y(), height.y()) + 1.0D;
        return Optional.of(new TriggerSelectionGeometry(AreaShapeKind.CYLINDER,
                new Vector3(c.X() - r, minY, c.Z() - r), new Vector3(c.X() + r, maxY, c.Z() + r),
                new Vector3(c.X(), (minY + maxY) / 2.0D, c.Z()), OptionalDouble.of(r), List.of()));
    }

    private static Optional<TriggerSelectionGeometry> polygon(List<TriggerBlockPoint> points) {
        List<Vector3> footprint = polygonFootprint(points);
        double minY = points.stream().mapToDouble(TriggerBlockPoint::y).min().orElseThrow();
        double highY = points.stream().mapToDouble(TriggerBlockPoint::y).max().orElseThrow();
        double maxY = highY == minY ? minY + 3.0D : highY + 1.0D;
        Vector3 min = new Vector3(extreme(footprint, true, true), minY, extreme(footprint, true, false));
        Vector3 max = new Vector3(extreme(footprint, false, true), maxY, extreme(footprint, false, false));
        return Optional.of(new TriggerSelectionGeometry(AreaShapeKind.POLYGON, min, max,
                min.add(max).divide(2.0D), OptionalDouble.empty(), footprint));
    }

    private static List<Vector3> polygonFootprint(List<TriggerBlockPoint> points) {
        double minY = points.stream().mapToDouble(TriggerBlockPoint::y).min().orElseThrow();
        return points.stream().map(point -> new Vector3(point.x(), minY, point.z())).toList();
    }

    private static Vector3 min(Vector3 a, Vector3 b) {
        return new Vector3(Math.min(a.X(), b.X()), Math.min(a.Y(), b.Y()), Math.min(a.Z(), b.Z()));
    }

    private static Vector3 max(Vector3 a, Vector3 b) {
        return new Vector3(Math.max(a.X(), b.X()), Math.max(a.Y(), b.Y()), Math.max(a.Z(), b.Z()));
    }

    private static double distance(Vector3 a, Vector3 b) {
        double dx = a.X() - b.X();
        double dy = a.Y() - b.Y();
        double dz = a.Z() - b.Z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double extreme(List<Vector3> points, boolean minimum, boolean xAxis) {
        return points.stream().mapToDouble(point -> xAxis ? point.X() : point.Z())
                .reduce(minimum ? Double::min : Double::max).orElseThrow();
    }
}
