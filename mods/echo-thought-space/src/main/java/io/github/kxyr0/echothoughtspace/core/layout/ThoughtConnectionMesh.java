package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import java.util.ArrayList;
import java.util.List;

/** A joined strip with the same front-facing winding as Minecraft's GUI fill. */
public final class ThoughtConnectionMesh {
    private ThoughtConnectionMesh() { }
    public record Quad(CanvasPoint first, CanvasPoint second, CanvasPoint third, CanvasPoint fourth) { }

    public static List<Quad> build(List<CanvasPoint> path) {
        List<Quad> result = new ArrayList<>();
        if (path.size() < 2) return result;
        CanvasPoint firstOffset = offset(path, 0);
        for (int index = 1; index < path.size(); index++) {
            CanvasPoint a = path.get(index - 1), b = path.get(index), secondOffset = offset(path, index);
            if (ThoughtConnectionRoute.length(a, b) > 0.000001D) {
                result.add(new Quad(shift(a, firstOffset, 1), shift(b, secondOffset, 1),
                        shift(b, secondOffset, -1), shift(a, firstOffset, -1)));
            }
            firstOffset = secondOffset;
        }
        return List.copyOf(result);
    }

    private static CanvasPoint shift(CanvasPoint point, CanvasPoint offset, int sign) {
        return new CanvasPoint(point.x() + offset.x() * sign, point.y() + offset.y() * sign);
    }

    private static CanvasPoint offset(List<CanvasPoint> path, int index) {
        CanvasPoint point = path.get(index);
        CanvasPoint before = path.get(Math.max(0, index - 1)), after = path.get(Math.min(path.size() - 1, index + 1));
        double beforeLength = Math.max(0.000001D, ThoughtConnectionRoute.length(before, point));
        double afterLength = Math.max(0.000001D, ThoughtConnectionRoute.length(point, after));
        double nx = -(point.y() - before.y()) / beforeLength - (after.y() - point.y()) / afterLength;
        double ny = (point.x() - before.x()) / beforeLength + (after.x() - point.x()) / afterLength;
        double length = Math.max(0.000001D, Math.hypot(nx, ny));
        nx /= length; ny /= length;
        double dx = index == 0 ? after.x() - point.x() : point.x() - before.x();
        double dy = index == 0 ? after.y() - point.y() : point.y() - before.y();
        double edgeLength = Math.max(0.000001D, Math.hypot(dx, dy));
        double miter = 1.5D / Math.max(0.5D, Math.abs((-dy * nx + dx * ny) / edgeLength));
        return new CanvasPoint(nx * miter, ny * miter);
    }
}
