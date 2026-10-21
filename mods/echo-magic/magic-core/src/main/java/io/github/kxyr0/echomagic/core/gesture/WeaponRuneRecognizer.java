package io.github.kxyr0.echomagic.core.gesture;

import java.util.List;

final class WeaponRuneRecognizer {
    private WeaponRuneRecognizer() { }

    static boolean matches(List<MagicGesturePoint> points, double width, double height,
                           double diagonal, double pathLength) {
        if (points.size() < 4 || diagonal < 0.48D || width < 0.25D || height < 0.25D
                || points.get(0).distanceTo(points.get(points.size() - 1)) > diagonal * 0.28D) return false;
        int firstCorner = pathIndex(points, pathLength / 3.0D);
        int secondCorner = pathIndex(points, pathLength * 2.0D / 3.0D);
        if (firstCorner < 1 || secondCorner <= firstCorner || secondCorner >= points.size() - 1) return false;
        MagicGesturePoint a = points.get(0); MagicGesturePoint b = points.get(firstCorner);
        MagicGesturePoint c = points.get(secondCorner);
        double tolerance = Math.max(0.055D, diagonal * 0.105D);
        for (int index = 0; index < points.size(); index++) {
            MagicGesturePoint start = index <= firstCorner ? a : index <= secondCorner ? b : c;
            MagicGesturePoint end = index <= firstCorner ? b : index <= secondCorner ? c : a;
            if (segmentDistance(points.get(index), start, end) > tolerance) return false;
        }
        return Math.abs(area(points)) >= width * height * 0.28D;
    }

    private static int pathIndex(List<MagicGesturePoint> points, double target) {
        double walked = 0.0D;
        int nearest = 0;
        double nearestDelta = Math.abs(target);
        for (int index = 1; index < points.size(); index++) {
            walked += points.get(index - 1).distanceTo(points.get(index));
            double delta = Math.abs(walked - target);
            if (delta < nearestDelta) { nearest = index; nearestDelta = delta; }
        }
        return nearest;
    }

    private static double segmentDistance(MagicGesturePoint point, MagicGesturePoint start,
                                          MagicGesturePoint end) {
        double dx = end.x() - start.x(); double dy = end.y() - start.y();
        double squared = dx * dx + dy * dy;
        if (squared <= 1.0E-8D) return point.distanceTo(start);
        double factor = Math.max(0.0D, Math.min(1.0D,
                ((point.x() - start.x()) * dx + (point.y() - start.y()) * dy) / squared));
        return Math.hypot(point.x() - start.x() - dx * factor, point.y() - start.y() - dy * factor);
    }

    private static double area(List<MagicGesturePoint> points) {
        double result = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            MagicGesturePoint a = points.get(index - 1); MagicGesturePoint b = points.get(index);
            result += a.x() * b.y() - b.x() * a.y();
        }
        return result * 0.5D;
    }
}
