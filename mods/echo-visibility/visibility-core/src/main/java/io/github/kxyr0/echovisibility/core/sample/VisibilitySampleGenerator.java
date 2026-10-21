package io.github.kxyr0.echovisibility.core.sample;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySamplePolicy;
import java.util.ArrayList;
import java.util.List;

public final class VisibilitySampleGenerator {
    public List<VisibilitySamplePoint> generate(VisibilityAabb bounds, VisibilitySamplePolicy policy) {
        List<VisibilitySamplePoint> samples = new ArrayList<>(policy.maximumSamples());
        for (VisibilitySampleKind kind : policy.kinds()) {
            add(samples, kind, bounds, policy.maximumSamples());
            if (samples.size() >= policy.maximumSamples()) {
                break;
            }
        }
        return List.copyOf(samples);
    }

    private static void add(
            List<VisibilitySamplePoint> samples,
            VisibilitySampleKind kind,
            VisibilityAabb bounds,
            int maximum) {
        switch (kind) {
            case NPC_HEAD -> add(samples, kind, vertical(bounds, 0.90D), maximum);
            case NPC_CHEST -> add(samples, kind, vertical(bounds, 0.68D), maximum);
            case NPC_PELVIS -> add(samples, kind, vertical(bounds, 0.43D), maximum);
            case NPC_FEET -> add(samples, kind, vertical(bounds, 0.06D), maximum);
            case BOUNDS_CENTER, BOX_CENTER -> add(samples, kind, bounds.center(), maximum);
            case BOUNDS_CORNERS, BOX_CORNERS -> bounds.corners()
                    .forEach(point -> add(samples, kind, point, maximum));
            case BOX_FACE_CENTERS -> faceCenters(bounds)
                    .forEach(point -> add(samples, kind, point, maximum));
        }
    }

    private static VisibilityVector3 vertical(VisibilityAabb bounds, double fraction) {
        VisibilityVector3 center = bounds.center();
        double y = bounds.min().y() + (bounds.max().y() - bounds.min().y()) * fraction;
        return new VisibilityVector3(center.x(), y, center.z());
    }

    private static List<VisibilityVector3> faceCenters(VisibilityAabb bounds) {
        VisibilityVector3 min = bounds.min();
        VisibilityVector3 max = bounds.max();
        VisibilityVector3 center = bounds.center();
        return List.of(
                new VisibilityVector3(min.x(), center.y(), center.z()),
                new VisibilityVector3(max.x(), center.y(), center.z()),
                new VisibilityVector3(center.x(), min.y(), center.z()),
                new VisibilityVector3(center.x(), max.y(), center.z()),
                new VisibilityVector3(center.x(), center.y(), min.z()),
                new VisibilityVector3(center.x(), center.y(), max.z()));
    }

    private static void add(
            List<VisibilitySamplePoint> samples,
            VisibilitySampleKind kind,
            VisibilityVector3 point,
            int maximum) {
        if (samples.size() < maximum) {
            samples.add(new VisibilitySamplePoint(kind, samples.size(), point));
        }
    }
}
