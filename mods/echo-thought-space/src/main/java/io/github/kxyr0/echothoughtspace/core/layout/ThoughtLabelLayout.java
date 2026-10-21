package io.github.kxyr0.echothoughtspace.core.layout;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/** Keeps wrapped labels near their owner and clear of all circles, in physical pixels. */
public final class ThoughtLabelLayout {
    public record Segment(CanvasPoint start, CanvasPoint end) { }

    public CanvasPoint place(CanvasPoint origin, double width, double height, double gap,
            CanvasPoint node, double radius, List<Segment> segments) {
        return place(origin, width, height, gap, node, radius, segments, List.of(node));
    }

    public CanvasPoint place(CanvasPoint origin, double width, double height, double gap,
            CanvasPoint node, double radius, List<Segment> segments, List<CanvasPoint> circles) {
        return place(origin, width, height, gap, node, radius, segments, circles, List.of());
    }

    public CanvasPoint place(CanvasPoint origin, double width, double height, double gap,
            CanvasPoint node, double radius, List<Segment> segments, List<CanvasPoint> circles, List<Box> occupied) {
        double halfWidth = width / 2.0D + gap;
        double halfHeight = height / 2.0D + gap;
        CanvasPoint preferred = new CanvasPoint(origin.x(), origin.y() + height / 2.0D);
        PriorityQueue<CanvasPoint> pending = new PriorityQueue<>(Comparator
                .comparingDouble((CanvasPoint point) -> distance(point, preferred))
                .thenComparingDouble(CanvasPoint::y).thenComparingDouble(CanvasPoint::x));
        Set<CanvasPoint> visited = new HashSet<>();
        pending.add(preferred);
        if (origin.x() != node.x()) pending.add(new CanvasPoint(2 * node.x() - preferred.x(), preferred.y()));
        double clearance = Math.max(gap + 1.0D, origin.y() - node.y() - radius);
        pending.add(new CanvasPoint(node.x(), node.y() - radius - clearance - height / 2.0D));
        CanvasPoint fallback = null;
        for (int attempt = 0; attempt < 128 && !pending.isEmpty(); attempt++) {
            CanvasPoint point = pending.remove();
            if (!visited.add(point)) continue;
            if (Math.abs(point.x() - node.x()) > Math.max(radius, Math.abs(origin.x() - node.x())) + 2.0D * gap
                    || Math.abs(point.y() - node.y()) > radius + clearance + height / 2.0D + 2.0D * gap) continue;
            CanvasPoint circle = circles.stream().filter(value -> overlapsCircle(point, halfWidth,
                    halfHeight, value, radius)).findFirst().orElse(null);
            if (circle != null) {
                separateBox(pending, point, circle.x() - radius, circle.x() + radius,
                        circle.y() - radius, circle.y() + radius, halfWidth, halfHeight);
                continue;
            }
            Box label = occupied.stream().filter(box -> overlapsBox(point, halfWidth, halfHeight, box))
                    .findFirst().orElse(null);
            if (label != null) {
                separateBox(pending, point, label.left(), label.right(), label.top(), label.bottom(), halfWidth, halfHeight);
                continue;
            }
            if (fallback == null) fallback = point;
            Segment obstruction = null;
            for (Segment segment : segments) {
                if (intersects(point, halfWidth, halfHeight, segment)) {
                    obstruction = segment;
                    break;
                }
            }
            if (obstruction == null) return new CanvasPoint(point.x(), point.y() - height / 2.0D);
            separate(pending, point, halfWidth, halfHeight, obstruction);
        }
        // Keep a nearby valid position when the line must route around the label.
        if (fallback != null) return new CanvasPoint(fallback.x(), fallback.y() - height / 2.0D);
        double top = node.y() - radius;
        for (CanvasPoint circle : circles) top = Math.min(top, circle.y() - radius);
        for (Box label : occupied) top = Math.min(top, label.top());
        return new CanvasPoint(origin.x(), top - height - gap - 1.0D);
    }

    public static boolean overlapsBox(CanvasPoint center, double halfWidth, double halfHeight, Box box) {
        return center.x() + halfWidth >= box.left() && center.x() - halfWidth <= box.right()
                && center.y() + halfHeight >= box.top() && center.y() - halfHeight <= box.bottom();
    }

    public static boolean overlapsCircle(CanvasPoint center, double halfWidth, double halfHeight,
            CanvasPoint circle, double radius) {
        double dx = Math.max(0.0D, Math.abs(center.x() - circle.x()) - halfWidth);
        double dy = Math.max(0.0D, Math.abs(center.y() - circle.y()) - halfHeight);
        return dx * dx + dy * dy <= radius * radius;
    }

    private static double distance(CanvasPoint point, CanvasPoint origin) {
        double dx = point.x() - origin.x();
        double dy = point.y() - origin.y();
        return dx * dx + dy * dy;
    }

    public static boolean intersects(CanvasPoint center, double halfWidth, double halfHeight, Segment segment) {
        double minX = Math.min(segment.start.x(), segment.end.x());
        double maxX = Math.max(segment.start.x(), segment.end.x());
        double minY = Math.min(segment.start.y(), segment.end.y());
        double maxY = Math.max(segment.start.y(), segment.end.y());
        if (maxX < center.x() - halfWidth || minX > center.x() + halfWidth
                || maxY < center.y() - halfHeight || minY > center.y() + halfHeight) return false;
        double nx = segment.start.y() - segment.end.y();
        double ny = segment.end.x() - segment.start.x();
        double projection = nx * (center.x() - segment.start.x()) + ny * (center.y() - segment.start.y());
        return Math.abs(projection) <= Math.abs(nx) * halfWidth + Math.abs(ny) * halfHeight;
    }

    private static void separate(PriorityQueue<CanvasPoint> pending, CanvasPoint point,
            double halfWidth, double halfHeight, Segment segment) {
        separateBox(pending, point, Math.min(segment.start.x(), segment.end.x()),
                Math.max(segment.start.x(), segment.end.x()), Math.min(segment.start.y(), segment.end.y()),
                Math.max(segment.start.y(), segment.end.y()), halfWidth, halfHeight);
        double nx = segment.start.y() - segment.end.y();
        double ny = segment.end.x() - segment.start.x();
        double length = Math.hypot(nx, ny);
        if (length < 0.001D) return;
        nx /= length;
        ny /= length;
        double projection = nx * (point.x() - segment.start.x()) + ny * (point.y() - segment.start.y());
        double extent = Math.abs(nx) * halfWidth + Math.abs(ny) * halfHeight + 1.0D;
        for (double side : new double[] {-extent, extent}) {
            double shift = side - projection;
            pending.add(new CanvasPoint(point.x() + nx * shift, point.y() + ny * shift));
        }
    }

    private static void separateBox(PriorityQueue<CanvasPoint> pending, CanvasPoint point,
            double left, double right, double top, double bottom, double halfWidth, double halfHeight) {
        pending.add(new CanvasPoint(left - halfWidth - 1.0D, point.y()));
        pending.add(new CanvasPoint(right + halfWidth + 1.0D, point.y()));
        pending.add(new CanvasPoint(point.x(), top - halfHeight - 1.0D));
        pending.add(new CanvasPoint(point.x(), bottom + halfHeight + 1.0D));
    }
}
