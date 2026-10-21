package io.github.kxyr0.echoplayersleep.core.recovery;

import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepRecoveryPolicy;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import java.util.Objects;

public record PlayerSleepRecoveryDecision(
        PlayerSleepRecoveryPolicy policy,
        boolean recoveryBlackoutRequired,
        PlayerSleepStatus nextStatus) {
    public PlayerSleepRecoveryDecision {
        policy = Objects.requireNonNull(policy, "policy");
        nextStatus = Objects.requireNonNull(nextStatus, "nextStatus");
    }

    public static PlayerSleepRecoveryDecision forSession(PlayerSleepSessionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean inDream = snapshot.status() == PlayerSleepStatus.DREAMING
                || snapshot.status() == PlayerSleepStatus.ENTERING_DREAM
                || snapshot.status() == PlayerSleepStatus.RETURNING;
        return new PlayerSleepRecoveryDecision(snapshot.recoveryPolicy(), inDream, PlayerSleepStatus.RECOVERING);
    }
}
