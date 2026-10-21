package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import java.util.ArrayList;
import java.util.List;

/** The accepted gesture, split at half its arc length for the new thought. */
public record ThoughtConnectionTrace(List<CanvasPoint> first, List<CanvasPoint> second) {
    public ThoughtConnectionTrace { first = List.copyOf(first); second = List.copyOf(second); }
    public CanvasPoint result() { return first.get(first.size() - 1); }

    public static ThoughtConnectionTrace split(List<CanvasPoint> path) {
        if (path.size() < 2) throw new IllegalArgumentException("A trace needs two endpoints.");
        double length = 0;
        for (int index = 1; index < path.size(); index++) length += ThoughtConnectionRoute.length(path.get(index - 1), path.get(index));
        double remaining = length / 2;
        List<CanvasPoint> first = new ArrayList<>();
        first.add(path.get(0));
        for (int index = 1; index < path.size(); index++) {
            CanvasPoint a = path.get(index - 1), b = path.get(index);
            double step = ThoughtConnectionRoute.length(a, b);
            if (remaining <= step) {
                double ratio = step < 0.000001D ? 0 : remaining / step;
                CanvasPoint middle = new CanvasPoint(a.x() + (b.x() - a.x()) * ratio, a.y() + (b.y() - a.y()) * ratio);
                first.add(middle);
                List<CanvasPoint> second = new ArrayList<>();
                second.add(middle);
                if (!middle.equals(b)) second.add(b);
                second.addAll(path.subList(index + 1, path.size()));
                return new ThoughtConnectionTrace(first, second);
            }
            remaining -= step;
            first.add(b);
        }
        throw new IllegalArgumentException("Invalid trace length.");
    }
}
