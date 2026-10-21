package io.github.kxyr0.echotrigger.api.geometry;

import java.util.List;

public record PolygonArea(List<Vector3> points, double minY, double maxY) implements AreaShape {
    private static final double EPSILON = 0.0000001D;

    public PolygonArea {
        points = List.copyOf(points);
        if (points.size() < 3 || points.size() > 128 || maxY <= minY || selfIntersects(points)) {
            throw new IllegalArgumentException("PolygonArea requires 3-128 non-intersecting points and valid Y.");
        }
        if (points.stream().anyMatch(point -> Math.abs(point.Y() - minY) > EPSILON)) {
            throw new IllegalArgumentException("PolygonArea point Y values must match minY.");
        }
    }

    @Override
    public AreaShapeKind kind() {
        return AreaShapeKind.POLYGON;
    }

    @Override
    public Vector3 min() {
        return new Vector3(extreme(true, true), minY, extreme(true, false));
    }

    @Override
    public Vector3 max() {
        return new Vector3(extreme(false, true), maxY, extreme(false, false));
    }

    @Override
    public Vector3 center() {
        return min().add(max()).divide(2.0D);
    }

    @Override
    public boolean contains(Vector3 point) {
        if (point.Y() < minY || point.Y() >= maxY) {
            return false;
        }
        return onAnyEdge(point) || rayIntersectsOdd(point);
    }

    private double extreme(boolean minimum, boolean xAxis) {
        return points.stream().mapToDouble(point -> xAxis ? point.X() : point.Z())
                .reduce(minimum ? Double::min : Double::max).orElseThrow();
    }

    private boolean rayIntersectsOdd(Vector3 point) {
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            Vector3 a = points.get(i);
            Vector3 b = points.get(j);
            boolean crosses = (a.Z() > point.Z()) != (b.Z() > point.Z());
            double atX = (b.X() - a.X()) * (point.Z() - a.Z()) / (b.Z() - a.Z()) + a.X();
            inside = crosses && point.X() < atX ? !inside : inside;
        }
        return inside;
    }

    private boolean onAnyEdge(Vector3 point) {
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            if (onSegment(points.get(j), point, points.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean selfIntersects(List<Vector3> checked) {
        for (int i = 0; i < checked.size(); i++) {
            if (edgeIntersectsOther(checked, i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean edgeIntersectsOther(List<Vector3> checked, int first) {
        for (int second = first + 1; second < checked.size(); second++) {
            if (!adjacent(checked.size(), first, second)
                    && segmentsIntersect(a(checked, first), b(checked, first),
                            a(checked, second), b(checked, second))) {
                return true;
            }
        }
        return false;
    }

    private static Vector3 a(List<Vector3> points, int edge) {
        return points.get(edge);
    }

    private static Vector3 b(List<Vector3> points, int edge) {
        return points.get((edge + 1) % points.size());
    }

    private static boolean adjacent(int size, int first, int second) {
        return Math.abs(first - second) == 1 || Math.abs(first - second) == size - 1;
    }

    private static boolean segmentsIntersect(Vector3 a, Vector3 b, Vector3 c, Vector3 d) {
        return direction(a, b, c) * direction(a, b, d) < 0.0D
                && direction(c, d, a) * direction(c, d, b) < 0.0D;
    }

    private static double direction(Vector3 a, Vector3 b, Vector3 c) {
        return (c.X() - a.X()) * (b.Z() - a.Z()) - (b.X() - a.X()) * (c.Z() - a.Z());
    }

    private static boolean onSegment(Vector3 a, Vector3 point, Vector3 b) {
        return Math.abs(direction(a, b, point)) <= EPSILON
                && point.X() >= Math.min(a.X(), b.X()) - EPSILON
                && point.X() <= Math.max(a.X(), b.X()) + EPSILON
                && point.Z() >= Math.min(a.Z(), b.Z()) - EPSILON
                && point.Z() <= Math.max(a.Z(), b.Z()) + EPSILON;
    }
}
