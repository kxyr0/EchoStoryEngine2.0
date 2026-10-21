package io.github.kxyr0.echonavigation.forge.api.service;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import java.util.Objects;
import java.util.Optional;

public interface NavigationService {
    MoveResult move(NavigationRequest request);

    MoveResult stop(NavigationSubjectId subjectId);

    MoveResult cancel(NavigationMoveId moveId);

    record MoveResult(Status status, Optional<NavigationSnapshot> snapshot, Optional<NavigationFailure> failure,
            String message) {
        public MoveResult {
            status = Objects.requireNonNull(status, "status");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            failure = Objects.requireNonNull(failure, "failure");
            message = Objects.requireNonNull(message, "message");
        }
    }

    enum Status {
        ACCEPTED,
        WAITING,
        DUPLICATE,
        CONFLICT,
        REJECTED,
        FAILED,
        UNAVAILABLE
    }
}
