package io.github.kxyr0.echoplayersleep.api.snapshot;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepControlMode;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepRecoveryPolicy;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepReturnMode;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.Objects;
import java.util.Optional;

public record PlayerSleepSessionSnapshot(
        PlayerSleepSessionId sessionId,
        PlayerSleepScenarioId scenarioId,
        PlayerRef player,
        PlayerSleepOwnerId owner,
        PlayerSleepBedSnapshot bed,
        PlayerSleepOriginSnapshot origin,
        PlayerSleepStatus status,
        PlayerSleepRevision revision,
        Optional<PlayerSleepDreamDestination> dreamDestination,
        PlayerSleepReturnMode returnMode,
        PlayerSleepRecoveryPolicy recoveryPolicy,
        PlayerSleepControlMode controlMode,
        boolean ownsBedPose,
        long sleepCount,
        long startedServerTick) {
    public PlayerSleepSessionSnapshot {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        player = Objects.requireNonNull(player, "player");
        owner = Objects.requireNonNull(owner, "owner");
        bed = Objects.requireNonNull(bed, "bed");
        origin = Objects.requireNonNull(origin, "origin");
        status = Objects.requireNonNull(status, "status");
        revision = Objects.requireNonNull(revision, "revision");
        dreamDestination = Objects.requireNonNull(dreamDestination, "dreamDestination");
        returnMode = Objects.requireNonNull(returnMode, "returnMode");
        recoveryPolicy = Objects.requireNonNull(recoveryPolicy, "recoveryPolicy");
        controlMode = Objects.requireNonNull(controlMode, "controlMode");
        if (sleepCount <= 0L) {
            throw new IllegalArgumentException("sleepCount must be positive.");
        }
    }
}
