package io.github.kxyr0.echoplayerhud.core.layout;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomically publishes last-known-good HUD layouts.
 */
public final class HudLayoutPublisher {
    private final AtomicReference<HudLayoutSnapshot> current;

    public HudLayoutPublisher(HudLayoutSnapshot initial) {
        this.current = new AtomicReference<>(Objects.requireNonNull(initial, "initial"));
    }

    public HudLayoutSnapshot current() {
        return current.get();
    }

    public void publish(HudLayoutSnapshot snapshot) {
        current.set(Objects.requireNonNull(snapshot, "snapshot"));
    }
}
