package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;

public record MovementPersistenceDto(
        NavigationMoveId moveId,
        NavigationCommandId commandId,
        NavigationExecutionId executionId,
        NavigationOwnerId ownerId,
        NavigationSubjectId subjectId,
        String dimensionId,
        int blockX,
        int blockY,
        int blockZ,
        NavigationStatus status,
        String failure,
        NavigationOutcome outcome,
        NavigationBackendId backendId,
        long settingsRevision,
        double currentX,
        double currentY,
        double currentZ,
        double bestDistance,
        long lastProgressTick,
        int repathCount,
        long movementTicks) {
}
