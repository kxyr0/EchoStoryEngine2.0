package io.github.kxyr0.echoplayersleep.forge.event;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventId;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecycleEvent;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

public final class PlayerSleepEventPublisher {
    private final Map<PlayerSleepEventId, PlayerSleepEventSnapshot> published = new LinkedHashMap<>();

    public Optional<PlayerSleepEventSnapshot> publish(
            PlayerSleepEventKind kind,
            PlayerSleepLifecyclePhase phase,
            PlayerSleepSessionSnapshot snapshot,
            long serverTick) {
        PlayerSleepEventSnapshot sleepEvent = PlayerSleepEventSnapshot.from(kind, snapshot, serverTick);
        if (published.containsKey(sleepEvent.eventId())) {
            return Optional.empty();
        }
        published.put(sleepEvent.eventId(), sleepEvent);
        MinecraftForge.EVENT_BUS.post(new PlayerSleepLifecycleEvent(phase, snapshot, Optional.of(sleepEvent)));
        return Optional.of(sleepEvent);
    }

    public void publishLifecycle(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        MinecraftForge.EVENT_BUS.post(new PlayerSleepLifecycleEvent(phase, snapshot));
    }

    public Optional<PlayerSleepEventSnapshot> event(PlayerSleepEventId eventId) {
        return Optional.ofNullable(published.get(eventId));
    }

    public List<PlayerSleepEventSnapshot> events() {
        return List.copyOf(published.values());
    }

    public int clearFor(PlayerRef player) {
        int before = published.size();
        published.values().removeIf(event -> event.player().equals(player));
        return before - published.size();
    }
}
