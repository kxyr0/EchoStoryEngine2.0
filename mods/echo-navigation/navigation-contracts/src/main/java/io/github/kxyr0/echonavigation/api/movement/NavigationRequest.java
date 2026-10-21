package io.github.kxyr0.echonavigation.api.movement;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import java.util.Objects;

public record NavigationRequest(
        NavigationMoveId moveId,
        NavigationCommandId commandId,
        NavigationExecutionId executionId,
        NavigationOwnerId ownerId,
        NavigationSubjectId subjectId,
        NavigationDestination destination,
        NavigationBackendId backendId,
        long settingsRevision,
        long commandGeneration,
        NavigationGait gait,
        double speedBlocksPerSecond,
        boolean synchronizePresentationSpeed) {
    public static final double LEGACY_SPEED_BLOCKS_PER_SECOND = 3.2D;

    public NavigationRequest(
            NavigationMoveId moveId,
            NavigationCommandId commandId,
            NavigationExecutionId executionId,
            NavigationOwnerId ownerId,
            NavigationSubjectId subjectId,
            NavigationDestination destination,
            NavigationBackendId backendId,
            long settingsRevision,
            long commandGeneration) {
        this(moveId, commandId, executionId, ownerId, subjectId, destination, backendId, settingsRevision,
                commandGeneration, NavigationGait.WALK, LEGACY_SPEED_BLOCKS_PER_SECOND, true);
    }

    public NavigationRequest {
        moveId = Objects.requireNonNull(moveId, "moveId");
        commandId = Objects.requireNonNull(commandId, "commandId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        destination = Objects.requireNonNull(destination, "destination");
        backendId = Objects.requireNonNull(backendId, "backendId");
        gait = Objects.requireNonNull(gait, "gait");
        if (settingsRevision < 1L || commandGeneration < 0L) {
            throw new IllegalArgumentException("Revisions must be positive and generation must not be negative.");
        }
        if (!Double.isFinite(speedBlocksPerSecond) || speedBlocksPerSecond <= 0.0D) {
            throw new IllegalArgumentException("speedBlocksPerSecond must be finite and positive.");
        }
    }
}
