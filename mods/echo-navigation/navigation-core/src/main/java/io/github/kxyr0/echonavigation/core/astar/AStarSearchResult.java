package io.github.kxyr0.echonavigation.core.astar;

import io.github.kxyr0.echonavigation.api.route.NavigationRoute;
import java.util.Objects;
import java.util.Optional;

public record AStarSearchResult(
        Status status,
        Optional<NavigationRoute> route,
        int expandedNodes,
        int openSetPeak,
        long planningNanos,
        String diagnostic) {
    public AStarSearchResult {
        status = Objects.requireNonNull(status, "status");
        route = Objects.requireNonNull(route, "route");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
        if (expandedNodes < 0 || openSetPeak < 0 || planningNanos < 0L) {
            throw new IllegalArgumentException("A* metrics must not be negative.");
        }
    }

    public enum Status {
        ROUTE_FOUND,
        PATH_NOT_FOUND,
        SEARCH_LIMIT_REACHED,
        ROUTE_RECONSTRUCTION_FAILED
    }
}
