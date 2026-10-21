package io.github.kxyr0.echoplayervitals.forge.api.client;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client bridge used by optional adapters without depending on Vitals runtime classes.
 */
public final class PlayerVitalsClientBridge {
    private static final AtomicReference<PlayerVitalsClientSnapshotView> VIEW = new AtomicReference<>();

    private PlayerVitalsClientBridge() {
    }

    public static void attach(PlayerVitalsClientSnapshotView view) {
        VIEW.set(Objects.requireNonNull(view, "view"));
    }

    public static Optional<PlayerVitalsClientSnapshotView> lookup() {
        return Optional.ofNullable(VIEW.get());
    }
}
