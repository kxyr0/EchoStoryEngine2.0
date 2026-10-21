package io.github.kxyr0.echoplayervitals.forge.api.lifecycle;

import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Public lifecycle event emitted after a Vitals snapshot is created or changed.
 */
public final class PlayerVitalsLifecycleEvent extends Event {
    private final PlayerVitalsSnapshot snapshot;
    private final PlayerVitalsLifecycleReason reason;

    public PlayerVitalsLifecycleEvent(PlayerVitalsSnapshot snapshot, PlayerVitalsLifecycleReason reason) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public PlayerVitalsSnapshot snapshot() {
        return snapshot;
    }

    public PlayerVitalsLifecycleReason reason() {
        return reason;
    }
}
