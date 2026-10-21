package io.github.kxyr0.echoplayersleep.core.arm;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepArmId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.Objects;

public record PlayerSleepArmSnapshot(
        PlayerSleepArmId armId,
        PlayerRef player,
        PlayerSleepScenarioId scenarioId,
        PlayerSleepOwnerId owner,
        PlayerSleepCommandId commandId,
        PlayerSleepRevision revision,
        long generation,
        long createdTick,
        boolean consumed) {
    public PlayerSleepArmSnapshot {
        armId = Objects.requireNonNull(armId, "armId");
        player = Objects.requireNonNull(player, "player");
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        owner = Objects.requireNonNull(owner, "owner");
        commandId = Objects.requireNonNull(commandId, "commandId");
        revision = Objects.requireNonNull(revision, "revision");
    }

    public PlayerSleepArmSnapshot markConsumed() {
        return new PlayerSleepArmSnapshot(armId, player, scenarioId, owner, commandId,
                revision.next(), generation, createdTick, true);
    }
}
