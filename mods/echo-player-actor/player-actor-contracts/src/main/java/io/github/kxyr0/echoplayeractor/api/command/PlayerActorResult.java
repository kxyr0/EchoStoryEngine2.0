package io.github.kxyr0.echoplayeractor.api.command;

import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import java.util.Objects;
import java.util.Optional;

public record PlayerActorResult(
        PlayerActorStatus status,
        PlayerActorOutcome outcome,
        Optional<PlayerActorFailure> failure,
        Optional<PlayerActorSnapshot> snapshot,
        String message) {
    public PlayerActorResult {
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        failure = Objects.requireNonNull(failure, "failure");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        message = Objects.requireNonNull(message, "message");
    }

    public static PlayerActorResult success(PlayerActorOutcome outcome, PlayerActorSnapshot snapshot, String message) {
        return new PlayerActorResult(PlayerActorStatus.SUCCEEDED, outcome, Optional.empty(), Optional.of(snapshot),
                message);
    }

    public static PlayerActorResult waiting(PlayerActorOutcome outcome, PlayerActorSnapshot snapshot, String message) {
        return new PlayerActorResult(PlayerActorStatus.WAITING, outcome, Optional.empty(), Optional.of(snapshot),
                message);
    }

    public static PlayerActorResult duplicate(PlayerActorOutcome outcome, String message) {
        return new PlayerActorResult(PlayerActorStatus.DUPLICATE, outcome, Optional.empty(), Optional.empty(), message);
    }

    public static PlayerActorResult rejected(PlayerActorFailure failure, String message) {
        return new PlayerActorResult(PlayerActorStatus.REJECTED, PlayerActorOutcome.NONE, Optional.of(failure),
                Optional.empty(), message);
    }

    public static PlayerActorResult unavailable(String message) {
        return new PlayerActorResult(PlayerActorStatus.UNAVAILABLE, PlayerActorOutcome.NONE, Optional.empty(),
                Optional.empty(), message);
    }
}
