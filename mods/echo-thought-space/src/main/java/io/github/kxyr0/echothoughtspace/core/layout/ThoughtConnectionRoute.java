package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import java.util.ArrayList;
import java.util.List;

public final class ThoughtConnectionRoute {
    private ThoughtConnectionRoute() { }

    public record Box(double left, double top, double right, double bottom) {
        public Box expand(double amount) { return new Box(left - amount, top - amount, right + amount, bottom + amount); }
        public boolean contains(CanvasPoint point) {
            return point.x() > left && point.x() < right && point.y() > top && point.y() < bottom;
        }
        public List<CanvasPoint> corners() {
            return List.of(new CanvasPoint(left, top), new CanvasPoint(right, top),
                    new CanvasPoint(right, bottom), new CanvasPoint(left, bottom));
        }
        public double distance(CanvasPoint point) {
            return Math.hypot(Math.max(0, Math.max(left - point.x(), point.x() - right)),
                    Math.max(0, Math.max(top - point.y(), point.y() - bottom)));
        }
        public CanvasPoint boundaryToward(CanvasPoint point) {
            return corners().stream().min(java.util.Comparator.comparingDouble(value -> length(value, point))).orElseThrow();
        }
    }

    public static double length(CanvasPoint a, CanvasPoint b) { return Math.hypot(a.x() - b.x(), a.y() - b.y()); }

    public static boolean blocked(CanvasPoint start, CanvasPoint end, List<Box> boxes) {
        for (Box box : boxes) if (crosses(start, end, box)) return true;
        return false;
    }

    public static boolean crosses(CanvasPoint start, CanvasPoint end, Box box) {
        double low = 0, high = 1;
        double[] from = {start.x(), start.y()};
        double[] delta = {end.x() - start.x(), end.y() - start.y()};
        double[] min = {box.left + 0.0001D, box.top + 0.0001D};
        double[] max = {box.right - 0.0001D, box.bottom - 0.0001D};
        for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(delta[axis]) < 0.000001D) {
                if (from[axis] < min[axis] || from[axis] > max[axis]) return false;
            } else {
                double first = (min[axis] - from[axis]) / delta[axis];
                double second = (max[axis] - from[axis]) / delta[axis];
                low = Math.max(low, Math.min(first, second));
                high = Math.min(high, Math.max(first, second));
                if (low > high) return false;
            }
        }
        return low <= high;
    }

    public static List<CanvasPoint> rounded(List<CanvasPoint> path, double radius, List<Box> boxes) {
        if (path.size() < 3 || radius < 0.1D) return path;
        List<CanvasPoint> result = new ArrayList<>();
        result.add(path.get(0));
        for (int index = 1; index < path.size() - 1; index++) {
            CanvasPoint corner = path.get(index);
            CanvasPoint before = path.get(index - 1), after = path.get(index + 1);
            double cut = Math.min(radius, Math.min(length(before, corner), length(corner, after)) * 0.45D);
            CanvasPoint entry = along(corner, before, cut), exit = along(corner, after, cut);
            result.add(entry);
            int samples = Math.max(8, (int) Math.ceil(cut / 2));
            for (int step = 1; step <= samples; step++) {
                double t = (double) step / samples, u = 1 - t;
                result.add(new CanvasPoint(u * u * entry.x() + 2 * u * t * corner.x() + t * t * exit.x(),
                        u * u * entry.y() + 2 * u * t * corner.y() + t * t * exit.y()));
            }
        }
        result.add(path.get(path.size() - 1));
        for (int index = 1; index < result.size(); index++) {
            if (blocked(result.get(index - 1), result.get(index), boxes)) return path;
        }
        return List.copyOf(result);
    }

    private static CanvasPoint along(CanvasPoint from, CanvasPoint to, double distance) {
        double ratio = distance / Math.max(0.000001D, length(from, to));
        return new CanvasPoint(from.x() + (to.x() - from.x()) * ratio, from.y() + (to.y() - from.y()) * ratio);
    }
}
