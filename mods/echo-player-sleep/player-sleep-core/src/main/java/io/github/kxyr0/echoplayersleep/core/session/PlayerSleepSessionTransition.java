package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSleepSessionTransition {
    private final PlayerSleepStateMachine stateMachine;

    public PlayerSleepSessionTransition(PlayerSleepStateMachine stateMachine) {
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
    }

    public PlayerSleepSessionSnapshot move(PlayerSleepSessionSnapshot snapshot, PlayerSleepStatus status) {
        return copy(snapshot, stateMachine.move(snapshot.status(), status), snapshot.dreamDestination(),
                snapshot.ownsBedPose());
    }

    public PlayerSleepSessionSnapshot dream(
            PlayerSleepSessionSnapshot snapshot,
            PlayerSleepDreamDestination destination) {
        return copy(snapshot, stateMachine.move(snapshot.status(), PlayerSleepStatus.ENTERING_DREAM),
                Optional.of(Objects.requireNonNull(destination, "destination")), snapshot.ownsBedPose());
    }

    public PlayerSleepSessionSnapshot bedPose(PlayerSleepSessionSnapshot snapshot, boolean ownsBedPose) {
        return copy(snapshot, snapshot.status(), snapshot.dreamDestination(), ownsBedPose);
    }

    public PlayerSleepSessionSnapshot force(PlayerSleepSessionSnapshot snapshot, PlayerSleepStatus status) {
        return copy(snapshot, Objects.requireNonNull(status, "status"), snapshot.dreamDestination(),
                snapshot.ownsBedPose());
    }

    private PlayerSleepSessionSnapshot copy(
            PlayerSleepSessionSnapshot snapshot,
            PlayerSleepStatus status,
            Optional<PlayerSleepDreamDestination> destination,
            boolean ownsBedPose) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new PlayerSleepSessionSnapshot(snapshot.sessionId(), snapshot.scenarioId(), snapshot.player(),
                snapshot.owner(), snapshot.bed(), snapshot.origin(), status, snapshot.revision().next(),
                destination, snapshot.returnMode(), snapshot.recoveryPolicy(), snapshot.controlMode(),
                ownsBedPose, snapshot.sleepCount(), snapshot.startedServerTick());
    }
}
