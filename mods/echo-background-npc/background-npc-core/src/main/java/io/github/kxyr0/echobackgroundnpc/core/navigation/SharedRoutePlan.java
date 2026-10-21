package io.github.kxyr0.echobackgroundnpc.core.navigation;

import java.util.List;
import java.util.Objects;

/** Immutable route geometry shared by compatible agents. */
public record SharedRoutePlan(RouteKey key, List<WorldPoint> points, double length) {
    public SharedRoutePlan {
        key = Objects.requireNonNull(key, "key");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        if (points.size() < 2 || !Double.isFinite(length) || length < 0.0) {
            throw new IllegalArgumentException("Invalid route plan.");
        }
    }

    public record RouteKey(long navigationRevision, String startAnchor, String targetAnchor,
            String movementProfile) {
        public RouteKey {
            startAnchor = require(startAnchor, "startAnchor");
            targetAnchor = require(targetAnchor, "targetAnchor");
            movementProfile = require(movementProfile, "movementProfile");
            if (navigationRevision < 0L) throw new IllegalArgumentException("Negative navigation revision.");
        }
        private static String require(String value, String name) {
            String checked = Objects.requireNonNull(value, name).trim();
            if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank.");
            return checked;
        }
    }
}
