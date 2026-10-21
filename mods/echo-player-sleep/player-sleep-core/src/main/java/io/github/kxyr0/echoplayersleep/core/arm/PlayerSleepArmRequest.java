package io.github.kxyr0.echoplayersleep.core.arm;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.Objects;

public record PlayerSleepArmRequest(
        PlayerRef player,
        PlayerSleepScenarioId scenarioId,
        PlayerSleepOwnerId owner,
        PlayerSleepCommandId commandId,
        long createdTick) {
    public PlayerSleepArmRequest {
        player = Objects.requireNonNull(player, "player");
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        owner = Objects.requireNonNull(owner, "owner");
        commandId = Objects.requireNonNull(commandId, "commandId");
    }
}
