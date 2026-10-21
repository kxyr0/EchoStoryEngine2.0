package io.github.kxyr0.echomagic.core.gesture.template;

import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import java.util.Arrays;
import java.util.List;

final class MagicGestureTemplateScoring {
    private static final double[] ROTATIONS = {-24.0D, -18.0D, -12.0D, -6.0D, 0.0D,
            6.0D, 12.0D, 18.0D, 24.0D};
    private static final int WARP_BAND = 10;

    private MagicGestureTemplateScoring() { }

    static double score(List<MagicGesturePoint> first, List<MagicGesturePoint> second) {
        double best = Double.POSITIVE_INFINITY;
        for (double degrees : ROTATIONS) best = Math.min(best, warpedScore(rotate(first, degrees), second));
        return best;
    }

    private static double warpedScore(List<MagicGesturePoint> first, List<MagicGesturePoint> second) {
        int count = MagicGestureTemplateLibrary.POINT_COUNT;
        double[] previous = new double[count + 1]; Arrays.fill(previous, Double.POSITIVE_INFINITY);
        previous[0] = 0.0D;
        for (int firstIndex = 1; firstIndex <= count; firstIndex++) {
            double[] current = new double[count + 1]; Arrays.fill(current, Double.POSITIVE_INFINITY);
            int minimum = Math.max(1, firstIndex - WARP_BAND);
            int maximum = Math.min(count, firstIndex + WARP_BAND);
            for (int secondIndex = minimum; secondIndex <= maximum; secondIndex++) {
                double predecessor = Math.min(previous[secondIndex - 1],
                        Math.min(previous[secondIndex], current[secondIndex - 1]));
                current[secondIndex] = predecessor + local(first, firstIndex - 1, second, secondIndex - 1);
            }
            previous = current;
        }
        return previous[count] / count;
    }

    private static double local(List<MagicGesturePoint> first, int firstIndex,
                                List<MagicGesturePoint> second, int secondIndex) {
        double position = first.get(firstIndex).distanceTo(second.get(secondIndex));
        double[] firstDirection = direction(first, firstIndex);
        double[] secondDirection = direction(second, secondIndex);
        double direction = (1.0D - firstDirection[0] * secondDirection[0]
                - firstDirection[1] * secondDirection[1]) * 0.5D;
        return position * 0.78D + direction * 0.22D;
    }

    private static double[] direction(List<MagicGesturePoint> points, int index) {
        MagicGesturePoint from = points.get(Math.max(0, index - 2));
        MagicGesturePoint to = points.get(Math.min(points.size() - 1, index + 2));
        double x = to.x() - from.x(); double y = to.y() - from.y(); double length = Math.hypot(x, y);
        return length < 1.0E-8D ? new double[] {0.0D, 0.0D} : new double[] {x / length, y / length};
    }

    private static List<MagicGesturePoint> rotate(List<MagicGesturePoint> points, double degrees) {
        double angle = Math.toRadians(degrees); double cosine = Math.cos(angle); double sine = Math.sin(angle);
        return points.stream().map(point -> new MagicGesturePoint(point.x() * cosine - point.y() * sine,
                point.x() * sine + point.y() * cosine, point.elapsedMillis())).toList();
    }
}
