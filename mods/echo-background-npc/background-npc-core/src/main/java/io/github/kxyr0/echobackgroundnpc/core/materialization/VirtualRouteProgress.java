package io.github.kxyr0.echobackgroundnpc.core.materialization;

import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;

/** Analytic offscreen route position used when an agent enters materialization range. */
public final class VirtualRouteProgress {
    public WorldPoint sample(SharedRoutePlan route, double progress) {
        double remaining = route.length() * Math.max(0.0, Math.min(1.0, progress));
        for (int index = 1; index < route.points().size(); index++) {
            WorldPoint start = route.points().get(index - 1);
            WorldPoint end = route.points().get(index);
            double segment = Math.sqrt(start.distanceSquared(end));
            if (remaining <= segment) return start.lerp(end, segment == 0.0 ? 1.0 : remaining / segment);
            remaining -= segment;
        }
        return route.points().get(route.points().size() - 1);
    }
}
