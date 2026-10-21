package io.github.kxyr0.echonavigation.api.movement;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;
import java.util.Optional;

public record NavigationSnapshot(
        NavigationMoveId moveId,
        NavigationCommandId commandId,
        NavigationExecutionId executionId,
        NavigationOwnerId ownerId,
        NavigationSubjectId subjectId,
        NavigationDestination destination,
        Vector3 resolvedTarget,
        NavigationStatus status,
        Optional<NavigationOutcome> outcome,
        Optional<NavigationFailure> failure,
        NavigationBackendId backendId,
        long settingsRevision,
        NavigationProgressSnapshot progress,
        NavigationGait gait,
        double speedBlocksPerSecond,
        boolean synchronizePresentationSpeed) {
    public NavigationSnapshot(
            NavigationMoveId moveId,
            NavigationCommandId commandId,
            NavigationExecutionId executionId,
            NavigationOwnerId ownerId,
            NavigationSubjectId subjectId,
            NavigationDestination destination,
            Vector3 resolvedTarget,
            NavigationStatus status,
            Optional<NavigationOutcome> outcome,
            Optional<NavigationFailure> failure,
            NavigationBackendId backendId,
            long settingsRevision,
            NavigationProgressSnapshot progress) {
        this(moveId, commandId, executionId, ownerId, subjectId, destination, resolvedTarget, status, outcome,
                failure, backendId, settingsRevision, progress, NavigationGait.WALK,
                NavigationRequest.LEGACY_SPEED_BLOCKS_PER_SECOND, true);
    }

    public NavigationSnapshot {
        moveId = Objects.requireNonNull(moveId, "moveId");
        commandId = Objects.requireNonNull(commandId, "commandId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        destination = Objects.requireNonNull(destination, "destination");
        resolvedTarget = Objects.requireNonNull(resolvedTarget, "resolvedTarget");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        failure = Objects.requireNonNull(failure, "failure");
        backendId = Objects.requireNonNull(backendId, "backendId");
        progress = Objects.requireNonNull(progress, "progress");
        gait = Objects.requireNonNull(gait, "gait");
        if (!Double.isFinite(speedBlocksPerSecond) || speedBlocksPerSecond <= 0.0D) {
            throw new IllegalArgumentException("speedBlocksPerSecond must be finite and positive.");
        }
    }
}
