package io.github.kxyr0.echonavigation.vanilla;

import io.github.kxyr0.echonavigation.api.backend.NavigationBackendDescriptor;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackend;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationPathView;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaBlockGridFollower;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaDoorSupport;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaLadderClimber;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaLadderPathPlanner;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaPathProgress;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaPathStarter;
import io.github.kxyr0.echonavigation.vanilla.validation.VanillaArrivalVerifier;
import io.github.kxyr0.echonavigation.vanilla.validation.VanillaDestinationValidator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VanillaNavigationBackend implements NavigationBackend {
    private static final NavigationBackendDescriptor DESCRIPTOR = new NavigationBackendDescriptor(
            NavigationBackendId.VANILLA, "Minecraft PathNavigation", 1, true,
            List.of("ground_mob_path", "bounded_replan", "path_snapshot", "door_opening", "block_grid_path"));
    private final VanillaDestinationValidator validator = new VanillaDestinationValidator();
    private final VanillaArrivalVerifier arrival = new VanillaArrivalVerifier();
    private final VanillaPathStarter starter = new VanillaPathStarter();
    private final VanillaPathProgress progress = new VanillaPathProgress();
    private final VanillaDoorSupport doors = new VanillaDoorSupport();
    private final VanillaLadderClimber ladders = new VanillaLadderClimber();
    private final VanillaBlockGridFollower blockGridFollower = new VanillaBlockGridFollower();
    private final VanillaLadderPathPlanner ladderPlanner = new VanillaLadderPathPlanner();
    private final Map<NavigationMoveId, VanillaMoveState> states = new LinkedHashMap<>();

    @Override
    public NavigationBackendId id() {
        return NavigationBackendId.VANILLA;
    }

    @Override
    public NavigationBackendDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public BackendStartResult start(BackendStartRequest request) {
        Optional<NavigationFailure> invalid = validator.validate(request.handle(), request.snapshot().destination());
        if (invalid.isPresent()) {
            return new BackendStartResult(Status.FAILED, invalid, "Destination rejected.");
        }
        VanillaPathStarter.Result started = startPath(request.handle(), request.snapshot().destination(),
                request.snapshot().speedBlocksPerSecond());
        if (started.failure().isPresent()) {
            return new BackendStartResult(Status.FAILED, started.failure(), started.message());
        }
        var pathSnapshot = progress.snapshot(request.handle(), request.snapshot().destination());
        states.put(request.snapshot().moveId(), new VanillaMoveState(request.serverTick(), pathSnapshot.nodes(), 0,
                request.snapshot().destination()));
        return new BackendStartResult(Status.ACCEPTED, Optional.empty(), started.message());
    }

    @Override
    public BackendTickResult tick(BackendTickRequest request) {
        NavigationMoveId moveId = request.snapshot().moveId();
        VanillaMoveState state = states.get(moveId);
        if (state == null) {
            return failure(request.handle(), NavigationFailure.NAVIGATION_VANILLA_PATH_INVALIDATED,
                    "Backend state missing.");
        }
        doors.configure(request.handle());
        doors.openNextDoor(request.handle());
        ladders.tick(request.handle());
        blockGridFollower.tick(request.handle());
        if (arrival.arrived(request.handle(), request.snapshot().destination(), request.settings())) {
            states.remove(moveId);
            var snapshot = progress.snapshot(request.handle(), request.snapshot().destination());
            return new BackendTickResult(Status.ARRIVED, Optional.of(NavigationOutcome.ARRIVED), Optional.empty(),
                    snapshot.current(), 0.0D, snapshot.pathIndex(), "Arrival verified.");
        }
        var snapshot = progress.snapshot(request.handle(), request.snapshot().destination());
        if (request.handle().mob().getNavigation().isDone()) {
            if (state.replans() >= request.settings().maxReplans()) {
                states.remove(moveId);
                return failure(request.handle(), NavigationFailure.NAVIGATION_VANILLA_PATH_INVALIDATED,
                        "PathNavigation ended before arrival and replan limit was reached.");
            }
            VanillaPathStarter.Result restarted = startPath(request.handle(), request.snapshot().destination(),
                    request.snapshot().speedBlocksPerSecond());
            if (restarted.failure().isPresent()) {
                states.remove(moveId);
                return failure(request.handle(), restarted.failure().orElseThrow(), restarted.message());
            }
            var restartedSnapshot = progress.snapshot(request.handle(), request.snapshot().destination());
            states.put(moveId, new VanillaMoveState(state.startTick(), restartedSnapshot.nodes(),
                    state.replans() + 1, state.destination()));
            return new BackendTickResult(Status.MOVING, Optional.empty(), Optional.empty(), restartedSnapshot.current(),
                    restartedSnapshot.remainingDistance(), restartedSnapshot.pathIndex(),
                    "PathNavigation ended before arrival; vanilla path replanned.");
        }
        return new BackendTickResult(Status.MOVING, Optional.empty(), Optional.empty(), snapshot.current(),
                snapshot.remainingDistance(), snapshot.pathIndex(), "Movement progressing.");
    }

    @Override
    public BackendCancelResult cancel(NavigationMoveId moveId, NavigationAgentHandle handle) {
        states.remove(moveId);
        handle.mob().getNavigation().stop();
        return new BackendCancelResult(true, "Vanilla navigation stopped.");
    }

    @Override
    public Optional<NavigationPathView> path(NavigationMoveId moveId) {
        VanillaMoveState state = states.get(moveId);
        if (state == null || state.destination == null) {
            return Optional.empty();
        }
        return Optional.of(new NavigationPathView(moveId, id(),
                state.nodes.isEmpty() ? state.destination.standingPosition() : state.nodes.get(0),
                state.destination, state.nodes, 0, NavigationStatus.MOVING, "none"));
    }

    private BackendTickResult failure(NavigationAgentHandle handle, NavigationFailure failure, String message) {
        return new BackendTickResult(Status.FAILED, Optional.of(NavigationOutcome.PATH_NOT_FOUND),
                Optional.of(failure), handle.position(), Double.MAX_VALUE, 0, message);
    }

    private VanillaPathStarter.Result startPath(NavigationAgentHandle handle,
            io.github.kxyr0.echonavigation.api.position.NavigationDestination destination,
            double speedBlocksPerSecond) {
        doors.configure(handle);
        double speedModifier = speedBlocksPerSecond
                / io.github.kxyr0.echonavigation.api.movement.NavigationRequest.LEGACY_SPEED_BLOCKS_PER_SECOND;
        VanillaPathStarter.Result started = starter.start(handle, destination, speedModifier);
        if (started.failure().isEmpty()
                || started.failure().orElseThrow() != NavigationFailure.NAVIGATION_VANILLA_PATH_NOT_FOUND) {
            return started;
        }
        return ladderPlanner.plan(handle, destination)
                .map(path -> starter.startPath(handle, path, speedModifier, "Block-grid path started."))
                .orElse(started);
    }

    private record VanillaMoveState(long startTick, List<io.github.kxyr0.echonavigation.api.position.Vector3> nodes,
            int replans, io.github.kxyr0.echonavigation.api.position.NavigationDestination destination) {
        VanillaMoveState(long startTick, List<io.github.kxyr0.echonavigation.api.position.Vector3> nodes,
                int replans) {
            this(startTick, nodes, replans, null);
        }
    }
}
