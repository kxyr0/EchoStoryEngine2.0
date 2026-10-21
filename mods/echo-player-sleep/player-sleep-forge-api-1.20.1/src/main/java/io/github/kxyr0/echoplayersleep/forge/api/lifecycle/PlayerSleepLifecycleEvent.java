package io.github.kxyr0.echoplayersleep.forge.api.lifecycle;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerSleepLifecycleEvent extends Event {
    private final PlayerSleepLifecyclePhase phase;
    private final PlayerSleepSessionSnapshot snapshot;
    private final Optional<PlayerSleepEventSnapshot> sleepEvent;

    public PlayerSleepLifecycleEvent(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        this(phase, snapshot, Optional.empty());
    }

    public PlayerSleepLifecycleEvent(
            PlayerSleepLifecyclePhase phase,
            PlayerSleepSessionSnapshot snapshot,
            Optional<PlayerSleepEventSnapshot> sleepEvent) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.sleepEvent = Objects.requireNonNull(sleepEvent, "sleepEvent");
    }

    public PlayerSleepLifecyclePhase phase() {
        return phase;
    }

    public PlayerSleepSessionSnapshot snapshot() {
        return snapshot;
    }

    public Optional<PlayerSleepEventSnapshot> sleepEvent() {
        return sleepEvent;
    }
}
