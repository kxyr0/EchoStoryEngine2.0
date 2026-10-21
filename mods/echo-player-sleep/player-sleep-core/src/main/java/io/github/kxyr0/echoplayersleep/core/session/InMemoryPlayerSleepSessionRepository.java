package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryPlayerSleepSessionRepository implements PlayerSleepSessionRepository {
    private final Map<PlayerSleepSessionId, PlayerSleepSessionSnapshot> sessions = new LinkedHashMap<>();

    @Override
    public boolean saveNew(PlayerSleepSessionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (activeFor(snapshot.player()).isPresent()) {
            return false;
        }
        sessions.put(snapshot.sessionId(), snapshot);
        return true;
    }

    @Override
    public void save(PlayerSleepSessionSnapshot snapshot) {
        sessions.put(Objects.requireNonNull(snapshot, "snapshot").sessionId(), snapshot);
    }

    @Override
    public Optional<PlayerSleepSessionSnapshot> find(PlayerSleepSessionId sessionId) {
        return Optional.ofNullable(sessions.get(Objects.requireNonNull(sessionId, "sessionId")));
    }

    @Override
    public Optional<PlayerSleepSessionSnapshot> activeFor(PlayerRef player) {
        return sessions.values().stream()
                .filter(snapshot -> snapshot.player().equals(player))
                .filter(snapshot -> !terminal(snapshot.status()))
                .findFirst();
    }

    @Override
    public List<PlayerSleepSessionSnapshot> activeSessions() {
        return sessions.values().stream().filter(snapshot -> !terminal(snapshot.status())).toList();
    }

    public int clearFor(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        int before = sessions.size();
        sessions.values().removeIf(snapshot -> snapshot.player().equals(player));
        return before - sessions.size();
    }

    private boolean terminal(PlayerSleepStatus status) {
        return status == PlayerSleepStatus.COMPLETED
                || status == PlayerSleepStatus.CANCELLED
                || status == PlayerSleepStatus.FAILED;
    }
}
