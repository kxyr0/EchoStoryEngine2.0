package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ThoughtConnectionRouter {
    public List<CanvasPoint> route(CanvasPoint start, CanvasPoint end, List<Box> labels, double bendRadius) {
        return route(start, end, labels, bendRadius, List.of());
    }

    public List<CanvasPoint> route(CanvasPoint start, CanvasPoint end, List<Box> labels,
            double bendRadius, List<CanvasPoint> preferred) {
        // A dragged pointer may be inside text: stop at its protected boundary.
        for (Box box : labels) {
            if (box.contains(start)) start = box.boundaryToward(end);
            if (box.contains(end)) end = box.boundaryToward(start);
        }
        if (preferred.size() > 1 && start.equals(preferred.get(0)) && end.equals(preferred.get(preferred.size() - 1))) {
            boolean clear = true;
            for (int index = 1; index < preferred.size(); index++) {
                if (ThoughtConnectionRoute.blocked(preferred.get(index - 1), preferred.get(index), labels)) clear = false;
            }
            if (clear) return preferred;
        }
        if (preferred.isEmpty() && !ThoughtConnectionRoute.blocked(start, end, labels)) return List.of(start, end);
        double padding = bendRadius;
        for (Box box : labels) padding = Math.min(padding, Math.min(box.distance(start), box.distance(end)) * 0.45D);
        final double expansion = padding;
        List<Box> obstacles = labels.stream().map(box -> box.expand(expansion)).toList();
        List<CanvasPoint> vertices = new ArrayList<>(List.of(start, end));
        for (Box box : obstacles) vertices.addAll(box.corners());
        vertices.addAll(preferred);
        double[] distance = new double[vertices.size()];
        int[] previous = new int[vertices.size()];
        boolean[] visited = new boolean[vertices.size()];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(previous, -1);
        distance[0] = 0;
        for (int step = 0; step < vertices.size(); step++) {
            int current = -1;
            for (int index = 0; index < vertices.size(); index++) {
                if (!visited[index] && (current < 0 || distance[index] < distance[current])) current = index;
            }
            if (current < 0 || !Double.isFinite(distance[current]) || current == 1) break;
            visited[current] = true;
            for (int next = 0; next < vertices.size(); next++) {
                if (visited[next] || ThoughtConnectionRoute.blocked(vertices.get(current), vertices.get(next), obstacles)) continue;
                var a = vertices.get(current); var b = vertices.get(next);
                var midpoint = new CanvasPoint((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
                double bias = 1 + 4 * deviation(midpoint, preferred) / Math.max(1, bendRadius);
                double candidate = distance[current] + ThoughtConnectionRoute.length(a, b) * bias;
                if (candidate < distance[next]) { distance[next] = candidate; previous[next] = current; }
            }
        }
        if (previous[1] < 0) return List.of();
        List<CanvasPoint> path = new ArrayList<>();
        for (int index = 1; index >= 0; index = previous[index]) path.add(vertices.get(index));
        Collections.reverse(path);
        return ThoughtConnectionRoute.rounded(path, padding, labels);
    }

    private static double deviation(CanvasPoint point, List<CanvasPoint> path) {
        if (path.size() < 2) return 0;
        double nearest = Double.POSITIVE_INFINITY;
        for (int index = 1; index < path.size(); index++) {
            var a = path.get(index - 1); var b = path.get(index);
            double dx = b.x() - a.x(), dy = b.y() - a.y();
            double ratio = Math.max(0, Math.min(1, ((point.x() - a.x()) * dx + (point.y() - a.y()) * dy)
                    / Math.max(0.000001D, dx * dx + dy * dy)));
            nearest = Math.min(nearest, Math.hypot(point.x() - a.x() - ratio * dx, point.y() - a.y() - ratio * dy));
        }
        return nearest;
    }
}
