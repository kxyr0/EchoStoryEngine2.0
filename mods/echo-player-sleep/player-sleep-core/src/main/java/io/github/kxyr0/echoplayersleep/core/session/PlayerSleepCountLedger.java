package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PlayerSleepCountLedger {
    private final Map<PlayerRef, Long> counts = new LinkedHashMap<>();

    public long recordConfirmedSleep(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        long next = counts.getOrDefault(player, 0L) + 1L;
        counts.put(player, next);
        return next;
    }

    public long count(PlayerRef player) {
        return counts.getOrDefault(Objects.requireNonNull(player, "player"), 0L);
    }

    public long reset(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        Long previous = counts.remove(player);
        return previous == null ? 0L : previous;
    }
}
