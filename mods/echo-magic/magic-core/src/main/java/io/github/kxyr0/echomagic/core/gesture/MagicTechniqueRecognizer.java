package io.github.kxyr0.echomagic.core.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import java.util.List;
import java.util.Optional;

public final class MagicTechniqueRecognizer {
    private static final double MINIMUM_LINE_LENGTH = 0.22D;
    private static final double LARGE_CIRCLE_AUTOMATIC_DIAGONAL = 0.90D;
    private static final long POINT_HOLD_MILLIS = 450L;
    private static final double POINT_RADIUS = 0.14D;

    public Optional<MagicTechnique> recognize(List<MagicGesturePoint> points) {
        if (!valid(points)) return Optional.empty();
        if (isHeldPoint(points)) return Optional.of(MagicTechnique.POINT);
        Metrics metrics = Metrics.from(points);
        if (WeaponRuneRecognizer.matches(points, metrics.width, metrics.height,
                metrics.diagonal, metrics.pathLength)) return Optional.of(MagicTechnique.WEAPON);
        if (isLargeCircle(points, metrics)) return Optional.of(MagicTechnique.CIRCLE);
        if (isHook(points, metrics)) return Optional.of(MagicTechnique.HOOK);
        if (isArc(points, metrics)) return Optional.of(MagicTechnique.ARC);
        return isLine(points, metrics) ? Optional.of(MagicTechnique.LINE) : Optional.empty();
    }

    public boolean isLine(List<MagicGesturePoint> points) {
        return valid(points) && isLine(points, Metrics.from(points));
    }

    public boolean isHeldPoint(List<MagicGesturePoint> points) {
        if (!valid(points)) return false;
        long duration = points.get(points.size() - 1).elapsedMillis()
                - points.get(0).elapsedMillis();
        if (duration < POINT_HOLD_MILLIS) return false;
        double centerX = points.stream().mapToDouble(MagicGesturePoint::x).average().orElse(0.0D);
        double centerY = points.stream().mapToDouble(MagicGesturePoint::y).average().orElse(0.0D);
        int stable = 0;
        for (MagicGesturePoint point : points) {
            if (Math.hypot(point.x() - centerX, point.y() - centerY) <= POINT_RADIUS) stable++;
        }
        return stable >= points.size() - Math.max(1, points.size() / 4);
    }

    private static boolean isLine(List<MagicGesturePoint> points, Metrics metrics) {
        MagicGesturePoint first = points.get(0);
        MagicGesturePoint last = points.get(points.size() - 1);
        double direct = first.distanceTo(last);
        if (direct < MINIMUM_LINE_LENGTH || metrics.pathLength <= 0.0D
                || direct / metrics.pathLength < 0.78D) return false;
        return maximumDeviation(points, first, last, direct) <= Math.max(0.042D, direct * 0.065D);
    }

    private static boolean isArc(List<MagicGesturePoint> points, Metrics metrics) {
        MagicGesturePoint first = points.get(0);
        MagicGesturePoint last = points.get(points.size() - 1);
        double direct = first.distanceTo(last);
        if (points.size() < 4 || direct < 0.3D || metrics.pathLength < direct * 1.025D
                || metrics.pathLength > direct * 2.0D) return false;
        double deviation = maximumDeviation(points, first, last, direct);
        if (deviation < Math.max(0.055D, direct * 0.075D) || deviation > direct * 0.78D) return false;
        double deltaX = last.x() - first.x();
        double deltaY = last.y() - first.y();
        int positive = 0;
        int negative = 0;
        double threshold = direct * 0.018D;
        for (int index = 1; index < points.size() - 1; index++) {
            MagicGesturePoint point = points.get(index);
            double cross = deltaX * (point.y() - first.y()) - deltaY * (point.x() - first.x());
            if (cross > threshold) positive++;
            else if (cross < -threshold) negative++;
        }
        int directional = positive + negative;
        return directional >= Math.max(1, (points.size() - 2) * 2 / 3)
                && Math.min(positive, negative) <= Math.max(1, directional / 6);
    }

    private static boolean isHook(List<MagicGesturePoint> points, Metrics metrics) {
        if (points.size() < 5 || metrics.height < 0.38D || metrics.width < 0.13D
                || metrics.width > metrics.height * 1.1D) return false;
        int bottomIndex = 0;
        for (int index = 1; index < points.size(); index++) {
            if (points.get(index).y() < points.get(bottomIndex).y()) bottomIndex = index;
        }
        if (bottomIndex < points.size() / 2 || bottomIndex >= points.size() - 1) return false;
        MagicGesturePoint first = points.get(0);
        MagicGesturePoint bottom = points.get(bottomIndex);
        MagicGesturePoint last = points.get(points.size() - 1);
        double stemMinX = Double.POSITIVE_INFINITY;
        double stemMaxX = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < bottomIndex; index++) {
            stemMinX = Math.min(stemMinX, points.get(index).x());
            stemMaxX = Math.max(stemMaxX, points.get(index).x());
        }
        return first.y() - bottom.y() >= metrics.height * 0.78D
                && last.y() - bottom.y() >= Math.max(0.11D, metrics.height * 0.16D)
                && Math.abs(last.x() - bottom.x()) >= Math.max(0.13D, metrics.height * 0.18D)
                && stemMaxX - stemMinX <= Math.max(0.1D, metrics.width * 0.42D)
                && first.distanceTo(last) >= metrics.diagonal * 0.35D;
    }

    private static boolean isLargeCircle(List<MagicGesturePoint> points, Metrics metrics) {
        if (metrics.diagonal < 0.52D || metrics.width <= 0.0D || metrics.height <= 0.0D) return false;
        double ratio = metrics.width / metrics.height;
        double endDistance = points.get(0).distanceTo(points.get(points.size() - 1));
        boolean automatic = metrics.diagonal >= LARGE_CIRCLE_AUTOMATIC_DIAGONAL;
        return (automatic || endHoldMillis(points, metrics.diagonal) >= 280L)
                && ratio >= 0.5D && ratio <= 2.0D
                && endDistance <= metrics.diagonal * 0.32D
                && metrics.pathLength >= metrics.diagonal * 1.85D
                && Math.abs(signedArea(points)) >= metrics.width * metrics.height * 0.32D;
    }

    private static long endHoldMillis(List<MagicGesturePoint> points, double diagonal) {
        int lastIndex = points.size() - 1;
        MagicGesturePoint last = points.get(lastIndex);
        double radius = Math.max(0.05D, diagonal * 0.09D);
        int holdStart = lastIndex;
        while (holdStart > 0 && points.get(holdStart - 1).distanceTo(last) <= radius) holdStart--;
        return last.elapsedMillis() - points.get(holdStart).elapsedMillis();
    }

    private static boolean valid(List<MagicGesturePoint> points) {
        if (points == null || points.size() < 2 || points.size() > MagicGestureRecognizer.MAXIMUM_POINTS) return false;
        long previous = -1L;
        for (MagicGesturePoint point : points) {
            if (point == null || point.elapsedMillis() < previous
                    || point.elapsedMillis() > MagicGestureRecognizer.MAXIMUM_DURATION_MILLIS) return false;
            previous = point.elapsedMillis();
        }
        return true;
    }

    private static double maximumDeviation(List<MagicGesturePoint> points, MagicGesturePoint first,
                                            MagicGesturePoint last, double direct) {
        double deltaX = last.x() - first.x();
        double deltaY = last.y() - first.y();
        double maximum = 0.0D;
        for (MagicGesturePoint point : points) {
            double cross = Math.abs(deltaX * (point.y() - first.y())
                    - deltaY * (point.x() - first.x()));
            maximum = Math.max(maximum, cross / direct);
        }
        return maximum;
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

    private record Metrics(double width, double height, double diagonal, double pathLength) {
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
            double width = maxX - minX;
            double height = maxY - minY;
            return new Metrics(width, height, Math.hypot(width, height), length);
        }
    }
}
