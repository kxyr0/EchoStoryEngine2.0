package io.github.kxyr0.echovisibility.core.math;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityScreenRectangle;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VisibilityFrustumClassifier {
    private static final int PLANE_COUNT = 6;
    private final VisibilityProjection projection = new VisibilityProjection();

    public VisibilityFrustumResult classify(
            VisibilityCameraSnapshot camera,
            VisibilityAabb bounds,
            int edgeMarginPixels) {
        if (camera.position() == null || bounds == null || edgeMarginPixels < 0) {
            return invalid();
        }
        List<VisibilityClipPoint> points = new ArrayList<>(8);
        for (var corner : bounds.corners()) {
            VisibilityClipPoint point = projection.project(camera, corner);
            if (!point.valid()) {
                return invalid();
            }
            points.add(point);
        }
        if (points.stream().allMatch(point -> point.w() <= 1.0E-7D)) {
            return new VisibilityFrustumResult(VisibilityFrustumClassification.BEHIND_CAMERA, Optional.empty());
        }
        double marginX = 2.0D * edgeMarginPixels / camera.framebufferWidth();
        double marginY = 2.0D * edgeMarginPixels / camera.framebufferHeight();
        boolean outside = outsideAnyPlane(points, marginX, marginY);
        Optional<VisibilityScreenRectangle> rectangle = rectangle(points);
        if (outside && !bounds.contains(camera.position())) {
            return new VisibilityFrustumResult(VisibilityFrustumClassification.FULLY_OUTSIDE, rectangle);
        }
        boolean allInside = points.stream().allMatch(point -> point.insideViewport(marginX, marginY));
        VisibilityFrustumClassification classification = allInside
                ? VisibilityFrustumClassification.FULLY_INSIDE
                : VisibilityFrustumClassification.INTERSECTING;
        return new VisibilityFrustumResult(classification, rectangle);
    }

    private static boolean outsideAnyPlane(
            List<VisibilityClipPoint> points,
            double marginX,
            double marginY) {
        for (int plane = 0; plane < PLANE_COUNT; plane++) {
            int outside = 0;
            for (VisibilityClipPoint point : points) {
                if (outside(point, plane, marginX, marginY)) {
                    outside++;
                }
            }
            if (outside == points.size()) {
                return true;
            }
        }
        return false;
    }

    private static boolean outside(VisibilityClipPoint point, int plane, double marginX, double marginY) {
        return switch (plane) {
            case 0 -> point.x() < -point.w() * (1.0D + marginX);
            case 1 -> point.x() > point.w() * (1.0D + marginX);
            case 2 -> point.y() < -point.w() * (1.0D + marginY);
            case 3 -> point.y() > point.w() * (1.0D + marginY);
            case 4 -> point.z() < -point.w();
            case 5 -> point.z() > point.w();
            default -> throw new IllegalArgumentException("Unknown frustum plane.");
        };
    }

    private static Optional<VisibilityScreenRectangle> rectangle(List<VisibilityClipPoint> points) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (VisibilityClipPoint point : points) {
            if (!point.inFront()) {
                continue;
            }
            minX = Math.min(minX, point.ndcX());
            minY = Math.min(minY, point.ndcY());
            maxX = Math.max(maxX, point.ndcX());
            maxY = Math.max(maxY, point.ndcY());
        }
        if (!Double.isFinite(minX)) {
            return Optional.empty();
        }
        return Optional.of(new VisibilityScreenRectangle(minX, minY, maxX, maxY));
    }

    private static VisibilityFrustumResult invalid() {
        return new VisibilityFrustumResult(VisibilityFrustumClassification.INVALID, Optional.empty());
    }
}
