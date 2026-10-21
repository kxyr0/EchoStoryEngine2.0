package io.github.kxyr0.echoplayersleep.forge.api.arm;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;

public interface PlayerSleepArmService {
    PlayerSleepResult arm(
            PlayerRef player,
            PlayerSleepScenarioId scenarioId,
            PlayerSleepOwnerId owner,
            PlayerSleepCommandId commandId);

    boolean disarm(PlayerRef player);

    boolean isArmed(PlayerRef player);

    boolean isArmed(PlayerRef player, PlayerSleepScenarioId scenarioId);
}
