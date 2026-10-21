package io.github.kxyr0.echoplayersleep.forge.api.session;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;

public interface PlayerSleepSessionService {
    PlayerSleepResult closeEyes(PlayerRef player, PlayerSleepCommandId commandId, Ticks duration);

    PlayerSleepResult waitUntilClosed(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult enterDream(PlayerRef player, PlayerSleepCommandId commandId,
            PlayerSleepDreamDestination destination);

    PlayerSleepResult beginBlackspaceDream(PlayerRef player, PlayerSleepCommandId commandId, String instanceId);

    PlayerSleepResult completeBlackspaceDream(PlayerRef player, PlayerSleepCommandId commandId);

    default PlayerSleepResult openEyes(PlayerRef player, PlayerSleepCommandId commandId,
            Ticks duration, boolean sleepyBlink) {
        return openEyes(player, commandId, duration, sleepyBlink, true);
    }

    PlayerSleepResult openEyes(PlayerRef player, PlayerSleepCommandId commandId,
            Ticks duration, boolean sleepyBlink, boolean showHud);

    PlayerSleepResult waitUntilOpen(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult returnToBed(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult beginBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult completeBlackspaceReturn(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult completeNight(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult complete(PlayerRef player, PlayerSleepCommandId commandId);

    PlayerSleepResult cancel(PlayerRef player, PlayerSleepCommandId commandId);
}
