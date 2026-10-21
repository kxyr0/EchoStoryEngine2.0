package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.core.movement.MovementStartResult;
import io.github.kxyr0.echonavigation.core.movement.NavigationRuntime;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationService;
import java.util.Optional;

final class ForgeMoveResults {
    private ForgeMoveResults() {
    }

    static NavigationService.MoveResult rejected(MovementStartResult started) {
        NavigationService.Status status = started.status() == MovementStartResult.Status.DUPLICATE_COMMAND
                ? NavigationService.Status.DUPLICATE
                : started.status() == MovementStartResult.Status.SUBJECT_BUSY
                        ? NavigationService.Status.CONFLICT : NavigationService.Status.REJECTED;
        return new NavigationService.MoveResult(status, started.snapshot(), started.failure(), started.status().name());
    }

    static NavigationService.MoveResult failed(NavigationRuntime runtime, NavigationMoveId moveId,
            NavigationFailure failure) {
        return failed(runtime, moveId, failure, failure.name());
    }

    static NavigationService.MoveResult failed(NavigationRuntime runtime, NavigationMoveId moveId,
            NavigationFailure failure, String message) {
        return new NavigationService.MoveResult(NavigationService.Status.FAILED, runtime.query(moveId),
                Optional.of(failure), message);
    }

    static NavigationOutcome outcome(NavigationFailure failure) {
        return switch (failure) {
            case NPC_MOVE_DESTINATION_NOT_STANDABLE, NPC_MOVE_DESTINATION_INVALID ->
                    NavigationOutcome.DESTINATION_INVALID;
            case NPC_MOVE_DESTINATION_UNLOADED -> NavigationOutcome.DESTINATION_UNLOADED;
            case NAVIGATION_BACKEND_UNAVAILABLE -> NavigationOutcome.BACKEND_UNAVAILABLE;
            default -> NavigationOutcome.PATH_NOT_FOUND;
        };
    }
}
