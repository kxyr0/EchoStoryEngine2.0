package io.github.kxyr0.echoplayersleep.forge.api.recovery;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;

public interface PlayerSleepRecoveryService {
    PlayerSleepResult recover(PlayerRef player);
}
