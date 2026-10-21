package io.github.kxyr0.echomagic.core.geometry;

import java.util.OptionalDouble;

public record SweptSegment(MagicPoint start, MagicPoint end) {
    public OptionalDouble firstIntersection(MagicAabb box) {
        double[] interval = {0.0D, 1.0D};
        if (!clip(start.x(), end.x() - start.x(), box.minX(), box.maxX(), interval)
                || !clip(start.y(), end.y() - start.y(), box.minY(), box.maxY(), interval)
                || !clip(start.z(), end.z() - start.z(), box.minZ(), box.maxZ(), interval)) return OptionalDouble.empty();
        return OptionalDouble.of(interval[0]);
    }

    private static boolean clip(double start, double delta, double min, double max, double[] interval) {
        if (Math.abs(delta) < 1.0E-12D) return start >= min && start <= max;
        double first = (min - start) / delta;
        double second = (max - start) / delta;
        if (first > second) { double swap = first; first = second; second = swap; }
        interval[0] = Math.max(interval[0], first);
        interval[1] = Math.min(interval[1], second);
        return interval[0] <= interval[1];
    }
}

