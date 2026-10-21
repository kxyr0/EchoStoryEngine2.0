package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmSnapshot;
import io.github.kxyr0.echoplayersleep.core.scenario.PlayerSleepScenarioDefinition;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSleepSessionFactory {
    public PlayerSleepSessionSnapshot create(
            PlayerSleepArmSnapshot arm,
            PlayerSleepScenarioDefinition scenario,
            VanillaSleepConfirmation confirmation,
            long sleepCount) {
        Objects.requireNonNull(arm, "arm");
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(confirmation, "confirmation");
        if (!arm.player().equals(confirmation.player())) {
            throw new IllegalArgumentException("arm and confirmation players differ");
        }
        if (sleepCount <= 0L) {
            throw new IllegalArgumentException("sleepCount must be positive.");
        }
        return new PlayerSleepSessionSnapshot(
                PlayerSleepSessionId.random(),
                arm.scenarioId(),
                arm.player(),
                arm.owner(),
                confirmation.bed(),
                confirmation.origin(),
                PlayerSleepStatus.VANILLA_SLEEP_CONFIRMED,
                PlayerSleepRevision.initial(),
                Optional.empty(),
                scenario.returnMode(),
                scenario.recoveryPolicy(),
                scenario.controlMode(),
                true,
                sleepCount,
                confirmation.serverTick());
    }
}
