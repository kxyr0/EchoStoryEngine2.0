package io.github.kxyr0.echoplayersleep.forge.api.session;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.List;
import java.util.Optional;

public interface PlayerSleepSessionQueryService {
    Optional<PlayerSleepSessionSnapshot> active(PlayerRef player);

    List<PlayerSleepSessionSnapshot> activeSessions();

    long sleepCount(PlayerRef player);
}
