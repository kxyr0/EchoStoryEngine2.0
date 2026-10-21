package io.github.kxyr0.echothoughtspace.core.canvas;

import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import java.util.List;
import java.util.Optional;

public final class ThoughtNodeHitTester {
    public Optional<ThoughtNodeHit> hit(List<ThoughtCanvasNode> nodes, CanvasTransform transform,
            CanvasPoint physicalPointer, double radiusReferencePixels) {
        double radius = radiusReferencePixels * transform.scale();
        ThoughtNodeHit best = null;
        for (int index = 0; index < nodes.size(); index++) {
            CanvasPoint center = transform.toPhysical(nodes.get(index).position());
            double dx = physicalPointer.x() - center.x();
            double dy = physicalPointer.y() - center.y();
            double distance = dx * dx + dy * dy;
            if (distance <= radius * radius && better(distance, index, best)) {
                best = new ThoughtNodeHit(nodes.get(index).snapshot().id(), distance, index);
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean better(double distance, int order, ThoughtNodeHit current) {
        return current == null || distance < current.distanceSquared()
                || distance == current.distanceSquared() && order > current.drawOrder();
    }
}

