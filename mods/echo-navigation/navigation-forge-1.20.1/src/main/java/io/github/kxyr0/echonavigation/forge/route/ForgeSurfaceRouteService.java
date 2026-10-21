package io.github.kxyr0.echonavigation.forge.route;

import io.github.kxyr0.echonavigation.core.astar.AStarRoutePlanner;
import io.github.kxyr0.echonavigation.core.astar.AStarSearchResult;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationSurfaceRouteService;
import io.github.kxyr0.echonavigation.vanilla.astar.surface.AStarWorldSnapshotFactory;

/** Route-only facade over the same immutable snapshot and A* planner used by the Mob backend. */
public final class ForgeSurfaceRouteService implements NavigationSurfaceRouteService {
    private final AStarWorldSnapshotFactory snapshots = new AStarWorldSnapshotFactory();
    private final AStarRoutePlanner planner = new AStarRoutePlanner();

    @Override
    public PlanResult plan(PlanRequest request) {
        if (!request.level().dimension().location().toString().equals(request.destination().dimensionId())) {
            return failed(Status.PATH_NOT_FOUND, "NAVIGATION_SUBJECT_WRONG_DIMENSION");
        }
        var snapshot = snapshots.create(request);
        if (snapshot.snapshot().isEmpty()) {
            Status status = snapshot.diagnostic().contains("LIMIT")
                    ? Status.SEARCH_LIMIT_REACHED : Status.PATH_NOT_FOUND;
            return failed(status, snapshot.diagnostic());
        }
        AStarSearchResult result = planner.plan(snapshot.snapshot().orElseThrow(), request.routeRevision());
        return new PlanResult(Status.valueOf(result.status().name()), result.route(), result.expandedNodes(),
                result.planningNanos(), result.diagnostic());
    }

    private PlanResult failed(Status status, String diagnostic) {
        return new PlanResult(status, java.util.Optional.empty(), 0, 0L, diagnostic);
    }
}
