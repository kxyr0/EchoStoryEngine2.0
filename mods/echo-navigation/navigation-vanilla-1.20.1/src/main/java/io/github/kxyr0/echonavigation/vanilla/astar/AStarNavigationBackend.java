package io.github.kxyr0.echonavigation.vanilla.astar;

import io.github.kxyr0.echonavigation.api.backend.NavigationBackendDescriptor;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.route.NavigationRoute;
import io.github.kxyr0.echonavigation.core.astar.AStarRoutePlanner;
import io.github.kxyr0.echonavigation.core.astar.AStarSearchResult;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackend;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationPathView;
import io.github.kxyr0.echonavigation.vanilla.astar.motor.NpcNavigationRouteMotor;
import io.github.kxyr0.echonavigation.vanilla.astar.surface.AStarWorldSnapshotFactory;
import io.github.kxyr0.echonavigation.vanilla.astar.surface.NavigationAgentProfile;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaDoorSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AStarNavigationBackend implements NavigationBackend {
    private static final NavigationBackendDescriptor DESCRIPTOR = new NavigationBackendDescriptor(
            NavigationBackendId.ASTAR, "Surface A* route motor", 1, true,
            List.of("surface_astar", "custom_motor", "stairs", "slabs", "ladders", "doors"));
    private final AStarWorldSnapshotFactory snapshots = new AStarWorldSnapshotFactory();
    private final AStarRoutePlanner planner = new AStarRoutePlanner();
    private final VanillaDoorSupport doors = new VanillaDoorSupport();
    private final Map<NavigationMoveId, State> states = new LinkedHashMap<>();

    @Override
    public NavigationBackendId id() {
        return NavigationBackendId.ASTAR;
    }

    @Override
    public NavigationBackendDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public BackendStartResult start(BackendStartRequest request) {
        if (!request.handle().dimensionId().equals(request.snapshot().destination().dimensionId())) {
            return failedStart(NavigationFailure.NAVIGATION_SUBJECT_WRONG_DIMENSION, "Wrong dimension.");
        }
        AStarWorldSnapshotFactory.Result snapshot = snapshots.create(request.snapshot(), request.handle());
        if (snapshot.snapshot().isEmpty()) {
            return failedStart(failure(snapshot.diagnostic()), snapshot.diagnostic());
        }
        AStarSearchResult result = planner.plan(snapshot.snapshot().orElseThrow(),
                request.snapshot().progress().repathCount() + 1L);
        if (result.route().isEmpty()) {
            return failedStart(searchFailure(result.status()), result.diagnostic());
        }
        NavigationRoute route = result.route().orElseThrow();
        NpcNavigationRouteMotor motor = new NpcNavigationRouteMotor(NavigationAgentProfile.from(request.handle()),
                request.snapshot().speedBlocksPerSecond());
        motor.start(route);
        request.handle().mob().getNavigation().stop();
        states.put(request.snapshot().moveId(), new State(route, motor, result));
        return new BackendStartResult(Status.ACCEPTED, Optional.empty(), acceptedMessage(result));
    }

    @Override
    public BackendTickResult tick(BackendTickRequest request) {
        State state = states.get(request.snapshot().moveId());
        if (state == null) {
            return failure(request.handle(), NavigationFailure.NAVIGATION_ASTAR_ROUTE_INVALIDATED,
                    "A* backend state missing.");
        }
        NpcNavigationRouteMotor.Tick tick = state.motor().tick(request.handle(), doors);
        if (tick.status() == NpcNavigationRouteMotor.Status.ARRIVED) {
            states.remove(request.snapshot().moveId());
            return new BackendTickResult(Status.ARRIVED, Optional.of(NavigationOutcome.ARRIVED), Optional.empty(),
                    request.handle().position(), 0.0D, tick.pathIndex(), "A* route arrived.");
        }
        if (tick.status() == NpcNavigationRouteMotor.Status.REPLAN_REQUESTED) {
            return new BackendTickResult(Status.REPLAN_REQUESTED, Optional.empty(), Optional.empty(),
                    tick.currentPosition(), tick.remainingDistance(), tick.pathIndex(),
                    "A* motor requested bounded replan.");
        }
        return new BackendTickResult(Status.MOVING, Optional.empty(), Optional.empty(),
                tick.currentPosition(), tick.remainingDistance(), tick.pathIndex(), tick.message());
    }

    @Override
    public BackendCancelResult cancel(NavigationMoveId moveId, NavigationAgentHandle handle) {
        Optional.ofNullable(states.remove(moveId)).ifPresent(state -> state.motor().stop(handle));
        handle.mob().getNavigation().stop();
        return new BackendCancelResult(true, "A* route motor stopped.");
    }

    @Override
    public Optional<NavigationPathView> path(NavigationMoveId moveId) {
        State state = states.get(moveId);
        if (state == null) {
            return Optional.empty();
        }
        NavigationRoute route = state.route();
        return Optional.of(new NavigationPathView(moveId, id(), route.nodes().get(0).stancePosition(),
                route.destination(), route.nodes().stream().map(node -> node.stancePosition()).toList(),
                state.motor().edgeIndex(), NavigationStatus.MOVING, "none"));
    }

    private BackendStartResult failedStart(NavigationFailure failure, String message) {
        return new BackendStartResult(Status.FAILED, Optional.of(failure), message);
    }

    private BackendTickResult failure(NavigationAgentHandle handle, NavigationFailure failure, String message) {
        return new BackendTickResult(Status.FAILED, Optional.of(NavigationOutcome.PATH_NOT_FOUND),
                Optional.of(failure), handle.position(), Double.MAX_VALUE, 0, message);
    }

    private NavigationFailure failure(String diagnostic) {
        if (diagnostic.startsWith("NAVIGATION_ASTAR_NO_DESTINATION_SURFACE")) {
            return NavigationFailure.NPC_MOVE_DESTINATION_NOT_STANDABLE;
        }
        if (diagnostic.startsWith("NAVIGATION_ASTAR_NO_START_SURFACE")) {
            return NavigationFailure.NPC_MOVE_START_REJECTED;
        }
        return diagnostic.startsWith("NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED")
                ? NavigationFailure.NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED : NavigationFailure.NPC_MOVE_PATH_NOT_FOUND;
    }

    private NavigationFailure searchFailure(AStarSearchResult.Status status) {
        return status == AStarSearchResult.Status.SEARCH_LIMIT_REACHED
                ? NavigationFailure.NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED
                : NavigationFailure.NPC_MOVE_PATH_NOT_FOUND;
    }

    private String acceptedMessage(AStarSearchResult result) {
        return "A* route started. expanded=" + result.expandedNodes()
                + ", peak=" + result.openSetPeak()
                + ", nanos=" + result.planningNanos();
    }

    private record State(NavigationRoute route, NpcNavigationRouteMotor motor, AStarSearchResult lastSearch) {
    }
}
