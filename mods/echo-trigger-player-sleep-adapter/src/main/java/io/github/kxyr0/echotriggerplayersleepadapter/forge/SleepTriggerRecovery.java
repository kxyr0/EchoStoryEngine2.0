package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventId;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SleepTriggerRecovery {
    public static final long DEFAULT_CLAIM_TIMEOUT_TICKS = 20L;
    private final Map<PlayerSleepEventId, Pending> pending = new LinkedHashMap<>();

    public void await(PlayerSleepEventSnapshot event, long currentTick) {
        pending.putIfAbsent(event.eventId(), new Pending(event, currentTick + DEFAULT_CLAIM_TIMEOUT_TICKS));
    }

    public List<Pending> pending() {
        return List.copyOf(pending.values());
    }

    public void remove(PlayerSleepEventId eventId) {
        pending.remove(Objects.requireNonNull(eventId, "eventId"));
    }

    public boolean expired(Pending pendingEvent, long currentTick) {
        return currentTick >= pendingEvent.deadlineTick();
    }

    public record Pending(PlayerSleepEventSnapshot event, long deadlineTick) {
        public Pending {
            event = Objects.requireNonNull(event, "event");
        }
    }
}
