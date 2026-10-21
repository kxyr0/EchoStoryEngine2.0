package io.github.kxyr0.echomagic.core.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import java.util.ArrayList;
import java.util.List;

public final class MagicGestureRecognizer {
    public static final int MAXIMUM_POINTS = 128;
    public static final long MAXIMUM_DURATION_MILLIS = 10_000L;
    private static final double MINIMUM_DRAW_SIZE = 0.24D;

    public static List<MagicGesturePoint> mergeStrokes(List<List<MagicGesturePoint>> strokes) {
        List<MagicGesturePoint> result = new ArrayList<>();
        if (strokes == null) return List.of();
        for (List<MagicGesturePoint> stroke : strokes) {
            if (stroke == null) continue;
            for (MagicGesturePoint point : stroke) {
                if (point != null && result.size() < MAXIMUM_POINTS) result.add(point);
            }
        }
        return List.copyOf(result);
    }

    public MagicGestureRecognition recognize(List<MagicGesturePoint> points) {
        if (!valid(points)) return MagicGestureRecognition.unknown();
        Metrics metrics = Metrics.from(points);
        long duration = points.get(points.size() - 1).elapsedMillis() - points.get(0).elapsedMillis();
        if (metrics.pathLength <= 0.05D && duration >= 600L) {
            return MagicGestureRecognition.recognized(MagicGestureSymbol.BLOOD, 1.0D);
        }
        if (metrics.diagonal < MINIMUM_DRAW_SIZE || metrics.pathLength < MINIMUM_DRAW_SIZE) {
            return MagicGestureRecognition.unknown();
        }
        if (isVoid(points, metrics)) return MagicGestureRecognition.recognized(MagicGestureSymbol.VOID, 0.95D);
        if (isAir(points, metrics)) return MagicGestureRecognition.recognized(MagicGestureSymbol.AIR, 0.9D);
        if (isWater(points, metrics)) return MagicGestureRecognition.recognized(MagicGestureSymbol.WATER, 0.88D);
        if (isEarth(points, metrics)) return MagicGestureRecognition.recognized(MagicGestureSymbol.EARTH, 0.86D);
        if (isFire(points, metrics, duration)) {
            return MagicGestureRecognition.recognized(MagicGestureSymbol.FIRE, 0.95D);
        }
        return MagicGestureRecognition.unknown();
    }

    private static boolean valid(List<MagicGesturePoint> points) {
        if (points == null || points.size() < 2 || points.size() > MAXIMUM_POINTS) return false;
        long previous = -1L;
        for (MagicGesturePoint point : points) {
            if (point == null || point.elapsedMillis() < previous
                    || point.elapsedMillis() > MAXIMUM_DURATION_MILLIS) return false;
            previous = point.elapsedMillis();
        }
        return true;
    }

    private static boolean isFire(List<MagicGesturePoint> points, Metrics metrics, long duration) {
        MagicGesturePoint first = points.get(0);
        MagicGesturePoint last = points.get(points.size() - 1);
        double netY = last.y() - first.y();
        double direct = first.distanceTo(last);
        return duration >= 320L
                && metrics.height >= Math.max(0.32D, metrics.width * 2.8D)
                && netY >= metrics.height * 0.88D && direct / metrics.pathLength >= 0.86D;
    }

    private static boolean isWater(List<MagicGesturePoint> points, Metrics metrics) {
        MagicGesturePoint first = points.get(0);
        MagicGesturePoint last = points.get(points.size() - 1);
        if (metrics.width < Math.max(0.38D, metrics.height * 1.9D)
                || metrics.height < 0.09D
                || Math.abs(last.x() - first.x()) < metrics.width * 0.78D) return false;
        int changes = 0;
        int previousSign = 0;
        double threshold = Math.max(0.008D, metrics.height * 0.035D);
        for (int index = 1; index < points.size(); index++) {
            double delta = points.get(index).y() - points.get(index - 1).y();
            int sign = delta > threshold ? 1 : delta < -threshold ? -1 : 0;
            if (sign != 0 && previousSign != 0 && sign != previousSign) changes++;
            if (sign != 0) previousSign = sign;
        }
        return changes >= 2 && metrics.pathLength >= metrics.width * 1.08D
                && metrics.pathLength <= metrics.width * 2.4D;
    }

    private static boolean isEarth(List<MagicGesturePoint> points, Metrics metrics) {
        MagicGesturePoint first = points.get(0);
        double bottom = metrics.minimumY;
        int bottomIndex = 1;
        while (bottomIndex < points.size() && points.get(bottomIndex).y() > bottom + metrics.height * 0.15D) {
            bottomIndex++;
        }
        if (bottomIndex >= points.size() - 1 || bottomIndex < points.size() / 4) return false;
        double footMinX = Double.POSITIVE_INFINITY;
        double footMaxX = Double.NEGATIVE_INFINITY;
        double footMinY = Double.POSITIVE_INFINITY;
        double footMaxY = Double.NEGATIVE_INFINITY;
        for (int index = bottomIndex; index < points.size(); index++) {
            MagicGesturePoint point = points.get(index);
            footMinX = Math.min(footMinX, point.x()); footMaxX = Math.max(footMaxX, point.x());
            footMinY = Math.min(footMinY, point.y()); footMaxY = Math.max(footMaxY, point.y());
        }
        double descent = points.get(bottomIndex).y() - first.y();
        return descent <= -metrics.height * 0.82D
                && footMaxX - footMinX >= Math.max(0.22D, metrics.height * 0.38D)
                && footMaxY - footMinY <= metrics.height * 0.18D;
    }

    private static boolean isAir(List<MagicGesturePoint> points, Metrics metrics) {
        double ratio = metrics.width / Math.max(metrics.height, 1.0E-8D);
        double endDistance = points.get(0).distanceTo(points.get(points.size() - 1));
        double area = Math.abs(signedArea(points));
        return ratio >= 0.62D && ratio <= 1.65D && endDistance <= metrics.diagonal * 0.22D
                && metrics.pathLength >= metrics.diagonal * 2.05D
                && area >= metrics.width * metrics.height * 0.42D;
    }

    private static boolean isVoid(List<MagicGesturePoint> points, Metrics metrics) {
        double primary = Math.max(metrics.width, metrics.height);
        double secondary = Math.min(metrics.width, metrics.height);
        MagicGesturePoint start = points.get(0);
        int farthestIndex = 0;
        double farthest = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            double distance = start.distanceTo(points.get(index));
            if (distance > farthest) { farthest = distance; farthestIndex = index; }
        }
        double endDistance = start.distanceTo(points.get(points.size() - 1));
        double returnLength = pathLength(points, farthestIndex, points.size() - 1);
        return primary >= 0.26D && secondary <= Math.max(0.10D, primary * 0.34D)
                && endDistance <= Math.max(0.09D, primary * 0.34D) && farthest >= primary * 0.82D
                && farthestIndex > 0 && farthestIndex < points.size() - 1
                && returnLength >= farthest * 0.58D && metrics.pathLength >= farthest * 1.55D;
    }

    private static double pathLength(List<MagicGesturePoint> points, int from, int to) {
        double length = 0.0D;
        for (int index = from + 1; index <= to; index++) {
            length += points.get(index - 1).distanceTo(points.get(index));
        }
        return length;
    }

    private static double signedArea(List<MagicGesturePoint> points) {
        double area = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            MagicGesturePoint previous = points.get(index - 1);
            MagicGesturePoint current = points.get(index);
            area += previous.x() * current.y() - current.x() * previous.y();
        }
        return area * 0.5D;
    }

    private record Metrics(double width, double height, double diagonal, double pathLength, double minimumY) {
        private static Metrics from(List<MagicGesturePoint> points) {
            double minX = Double.POSITIVE_INFINITY; double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY; double maxY = Double.NEGATIVE_INFINITY;
            double length = 0.0D;
            for (int index = 0; index < points.size(); index++) {
                MagicGesturePoint point = points.get(index);
                minX = Math.min(minX, point.x()); maxX = Math.max(maxX, point.x());
                minY = Math.min(minY, point.y()); maxY = Math.max(maxY, point.y());
                if (index > 0) length += point.distanceTo(points.get(index - 1));
            }
            return new Metrics(maxX - minX, maxY - minY, Math.hypot(maxX - minX, maxY - minY), length, minY);
        }
    }
}
