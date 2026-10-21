package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.core.movement.MovementStartResult;
import io.github.kxyr0.echonavigation.core.movement.NavigationRuntime;
import io.github.kxyr0.echonavigation.core.progress.NavigationProgressTracker;
import io.github.kxyr0.echonavigation.core.progress.NavigationStuckDetector;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolution;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackend;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackendView;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationQueryService;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationService;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationDiagnosticsView;
import io.github.kxyr0.echonavigation.forge.persistence.NavigationSavedDataCodec;
import java.util.List;
import java.util.Optional;

public final class ForgeNavigationService implements NavigationService, NavigationQueryService {
    private final ForgeNavigationHost host;
    private final NavigationRuntime runtime = new NavigationRuntime();
    private final NavigationProgressTracker progressTracker = new NavigationProgressTracker();
    private final NavigationStuckDetector stuckDetector = new NavigationStuckDetector();
    private final NavigationDiagnosticsBuffer diagnostics = new NavigationDiagnosticsBuffer();
    private final NavigationLifecyclePublisher events = new NavigationLifecyclePublisher();

    ForgeNavigationService(ForgeNavigationHost host) {
        this.host = host;
    }

    @Override
    public MoveResult move(NavigationRequest request) {
        NavigationAgentResolution resolved = resolve(request.subjectId());
        var initialPosition = resolved.handle().map(NavigationAgentHandle::position).orElseGet(() -> request.destination().standingPosition());
        MovementStartResult started = runtime.start(request, initialPosition, host.settings(), host.server().overworld().getGameTime());
        if (started.status() != MovementStartResult.Status.ACCEPTED) {
            return ForgeMoveResults.rejected(started);
        }
        host.flush();
        MoveResult dispatched = dispatch(started.snapshot().orElseThrow(), false);
        if (dispatched.status() == Status.ACCEPTED) {
            dispatched.snapshot().ifPresent(events::started);
        }
        host.flush();
        return dispatched;
    }

    @Override
    public MoveResult stop(NavigationSubjectId subjectId) {
        Optional<NavigationSnapshot> active = runtime.active(subjectId);
        return active.map(snapshot -> cancel(snapshot.moveId())).orElseGet(() -> new MoveResult(Status.REJECTED,
                Optional.empty(), Optional.empty(), "No active movement."));
    }

    @Override
    public MoveResult cancel(NavigationMoveId moveId) {
        Optional<NavigationSnapshot> existing = runtime.query(moveId);
        if (existing.isEmpty()) {
            return new MoveResult(Status.REJECTED, Optional.empty(), Optional.empty(), "Movement not found.");
        }
        existing.ifPresent(this::cancelBackend);
        var result = runtime.cancel(moveId, NavigationOutcome.CANCELLED);
        events.stopped(result.snapshot());
        host.flush();
        return new MoveResult(Status.ACCEPTED, Optional.of(result.snapshot()),
                Optional.of(NavigationFailure.NPC_MOVE_CANCELLED), "Movement cancelled.");
    }

    void tick(long serverTick) {
        for (NavigationSnapshot snapshot : runtime.list(512)) {
            if (snapshot.status().terminal()) {
                continue;
            }
            tickOne(snapshot, serverTick);
        }
        host.flush();
    }

    private void tickOne(NavigationSnapshot snapshot, long serverTick) {
        NavigationAgentResolution resolved = resolve(snapshot.subjectId());
        if (resolved.status() != NavigationAgentResolution.Status.RESOLVED) {
            runtime.status(snapshot.moveId(), NavigationStatus.SUSPENDED_UNLOADED);
            return;
        }
        if (snapshot.status() != NavigationStatus.MOVING && snapshot.status() != NavigationStatus.REPLANNING) {
            dispatch(snapshot, true);
            return;
        }
        NavigationBackend backend = backend(snapshot).orElse(null);
        if (backend == null) {
            fail(snapshot, NavigationOutcome.BACKEND_UNAVAILABLE, NavigationFailure.NAVIGATION_BACKEND_UNAVAILABLE);
            return;
        }
        var handle = resolved.handle().orElseThrow();
        var tick = backend.tick(new NavigationBackend.BackendTickRequest(snapshot, handle, host.settings(), serverTick));
        var progress = progressTracker.sample(snapshot.progress(), tick.currentPosition(), tick.remainingDistance(), tick.pathIndex(), serverTick, host.settings());
        var progressed = runtime.progress(snapshot.moveId(), progress.progress()).snapshot();
        events.progressed(progressed);
        if (tick.status() == NavigationBackend.Status.ARRIVED) {
            events.arrived(runtime.arrived(snapshot.moveId()).snapshot());
        } else if (tick.status() == NavigationBackend.Status.FAILED) {
            fail(snapshot, tick.outcome().orElse(NavigationOutcome.PATH_NOT_FOUND), tick.failure().orElse(NavigationFailure.NPC_MOVE_PATH_NOT_FOUND));
        } else if (tick.status() == NavigationBackend.Status.REPLAN_REQUESTED) {
            handleStuck(snapshot, backend, handle, serverTick);
        } else if (stuckDetector.stuck(snapshot.status(), progress.progress(), serverTick, host.settings())) {
            handleStuck(snapshot, backend, handle, serverTick);
        }
    }

    private MoveResult dispatch(NavigationSnapshot snapshot, boolean recovery) {
        NavigationAgentResolution resolved = resolve(snapshot.subjectId());
        if (resolved.status() != NavigationAgentResolution.Status.RESOLVED) {
            runtime.status(snapshot.moveId(), NavigationStatus.WAITING_FOR_MATERIALIZATION);
            return new MoveResult(Status.WAITING, runtime.query(snapshot.moveId()), Optional.empty(),
                    resolved.message());
        }
        Optional<NavigationBackend> backend = backend(snapshot);
        if (backend.isEmpty()) {
            fail(snapshot, NavigationOutcome.BACKEND_UNAVAILABLE, NavigationFailure.NAVIGATION_BACKEND_UNAVAILABLE);
            return ForgeMoveResults.failed(runtime, snapshot.moveId(), NavigationFailure.NAVIGATION_BACKEND_UNAVAILABLE);
        }
        runtime.status(snapshot.moveId(), NavigationStatus.PLANNING);
        var request = new NavigationBackend.BackendStartRequest(snapshot, resolved.handle().orElseThrow(),
                host.settings(), host.server().overworld().getGameTime());
        var started = recovery ? backend.orElseThrow().recover(request) : backend.orElseThrow().start(request);
        if (started.status() != NavigationBackend.Status.ACCEPTED) {
            NavigationFailure failure = started.failure().orElse(NavigationFailure.NPC_MOVE_START_REJECTED);
            fail(snapshot, ForgeMoveResults.outcome(failure), failure);
            return ForgeMoveResults.failed(runtime, snapshot.moveId(), failure, started.message());
        }
        var moving = runtime.status(snapshot.moveId(), NavigationStatus.MOVING).snapshot();
        return new MoveResult(Status.ACCEPTED, Optional.of(moving), Optional.empty(), started.message());
    }

    private void handleStuck(NavigationSnapshot snapshot, NavigationBackend backend, NavigationAgentHandle handle, long serverTick) {
        if (snapshot.progress().repathCount() >= host.settings().maxReplans()) {
            fail(snapshot, NavigationOutcome.STUCK, NavigationFailure.NPC_MOVE_STUCK); return;
        }
        var replanned = runtime.replan(snapshot.moveId()).snapshot();
        var started = backend.start(new NavigationBackend.BackendStartRequest(replanned, handle, host.settings(), serverTick));
        if (started.status() == NavigationBackend.Status.ACCEPTED) {
            runtime.status(snapshot.moveId(), NavigationStatus.MOVING); return;
        }
        NavigationFailure failure = started.failure().orElse(NavigationFailure.NPC_MOVE_PATH_NOT_FOUND); fail(replanned, ForgeMoveResults.outcome(failure), failure);
    }

    private void cancelBackend(NavigationSnapshot snapshot) {
        resolve(snapshot.subjectId()).handle().ifPresent(handle -> backend(snapshot)
                .ifPresent(backend -> backend.cancel(snapshot.moveId(), handle)));
    }

    private Optional<NavigationBackend> backend(NavigationSnapshot snapshot) { return host.backendRegistry().backend(snapshot.backendId()); }

    private NavigationAgentResolution resolve(NavigationSubjectId subjectId) {
        return host.resolvers().stream()
                .map(resolver -> resolver.resolve(host.server(), subjectId))
                .filter(result -> result.status() != NavigationAgentResolution.Status.SUBJECT_NOT_FOUND)
                .findFirst()
                .orElseGet(() -> NavigationAgentResolution.missing(
                        NavigationAgentResolution.Status.SUBJECT_NOT_FOUND, "Navigation subject not found."));
    }

    private void fail(NavigationSnapshot snapshot, NavigationOutcome outcome, NavigationFailure failure) {
        var failed = runtime.fail(snapshot.moveId(), outcome == NavigationOutcome.STUCK ? NavigationStatus.BLOCKED : NavigationStatus.FAILED, outcome, failure).snapshot();
        events.failed(failed);
        addDiagnostic(NavigationFailureDiagnostic.from(snapshot, failure));
    }

    void addDiagnostic(NavigationDiagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    List<String> encodedSnapshots() {
        NavigationSavedDataCodec codec = new NavigationSavedDataCodec();
        return runtime.list(1024).stream().map(codec::encode).toList();
    }

    @Override
    public boolean isMoving(NavigationSubjectId subjectId) {
        return runtime.isMoving(subjectId);
    }

    @Override
    public Optional<NavigationSnapshot> active(NavigationSubjectId subjectId) {
        return runtime.active(subjectId);
    }

    @Override
    public Optional<NavigationSnapshot> move(NavigationMoveId moveId) {
        return runtime.query(moveId);
    }

    @Override
    public List<NavigationSnapshot> list(int limit) {
        return runtime.list(limit);
    }

    @Override
    public List<NavigationBackendView> backends() {
        return host.backendRegistry().ids().stream()
                .flatMap(id -> host.backendRegistry().backend(id).stream())
                .map(backend -> new NavigationBackendView(backend.id(), backend.descriptor().displayName(),
                        backend.descriptor().schemaVersion(), backend.descriptor().available()))
                .toList();
    }

    @Override
    public io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot settings() {
        return host.settings();
    }

    @Override
    public NavigationDiagnosticsView diagnostics() {
        return diagnostics.view();
    }
}
