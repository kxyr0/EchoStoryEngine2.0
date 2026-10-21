package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.List;
import java.util.Optional;

public interface PlayerSleepSessionRepository {
    boolean saveNew(PlayerSleepSessionSnapshot snapshot);

    void save(PlayerSleepSessionSnapshot snapshot);

    Optional<PlayerSleepSessionSnapshot> find(PlayerSleepSessionId sessionId);

    Optional<PlayerSleepSessionSnapshot> activeFor(PlayerRef player);

    List<PlayerSleepSessionSnapshot> activeSessions();
}
