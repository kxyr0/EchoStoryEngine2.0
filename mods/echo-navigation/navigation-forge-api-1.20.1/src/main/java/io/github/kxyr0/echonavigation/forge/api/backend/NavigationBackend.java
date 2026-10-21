package io.github.kxyr0.echonavigation.forge.api.backend;

import io.github.kxyr0.echonavigation.api.backend.NavigationBackendDescriptor;
import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationPathView;
import java.util.Objects;
import java.util.Optional;

public interface NavigationBackend {
    NavigationBackendId id();

    NavigationBackendDescriptor descriptor();

    BackendStartResult start(BackendStartRequest request);

    BackendTickResult tick(BackendTickRequest request);

    BackendCancelResult cancel(NavigationMoveId moveId, NavigationAgentHandle handle);

    default BackendStartResult recover(BackendStartRequest request) {
        return start(request);
    }

    Optional<NavigationPathView> path(NavigationMoveId moveId);

    record BackendStartRequest(
            NavigationSnapshot snapshot,
            NavigationAgentHandle handle,
            NavigationSettingsSnapshot settings,
            long serverTick) {
        public BackendStartRequest {
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            handle = Objects.requireNonNull(handle, "handle");
            settings = Objects.requireNonNull(settings, "settings");
        }
    }

    record BackendTickRequest(
            NavigationSnapshot snapshot,
            NavigationAgentHandle handle,
            NavigationSettingsSnapshot settings,
            long serverTick) {
        public BackendTickRequest {
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            handle = Objects.requireNonNull(handle, "handle");
            settings = Objects.requireNonNull(settings, "settings");
        }
    }

    record BackendStartResult(Status status, Optional<NavigationFailure> failure, String message) {
        public BackendStartResult {
            status = Objects.requireNonNull(status, "status");
            failure = Objects.requireNonNull(failure, "failure");
            message = Objects.requireNonNull(message, "message");
        }
    }

    record BackendTickResult(
            Status status,
            Optional<NavigationOutcome> outcome,
            Optional<NavigationFailure> failure,
            Vector3 currentPosition,
            double remainingDistance,
            int pathIndex,
            String message) {
        public BackendTickResult {
            status = Objects.requireNonNull(status, "status");
            outcome = Objects.requireNonNull(outcome, "outcome");
            failure = Objects.requireNonNull(failure, "failure");
            currentPosition = Objects.requireNonNull(currentPosition, "currentPosition");
            message = Objects.requireNonNull(message, "message");
        }
    }

    record BackendCancelResult(boolean accepted, String message) {
    }

    enum Status {
        ACCEPTED,
        MOVING,
        ARRIVED,
        CANCELLED,
        FAILED,
        REPLAN_REQUESTED
    }
}
