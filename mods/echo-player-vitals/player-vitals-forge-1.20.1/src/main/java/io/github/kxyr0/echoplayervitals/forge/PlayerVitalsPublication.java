package io.github.kxyr0.echoplayervitals.forge;

import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.forge.api.lifecycle.PlayerVitalsLifecycleReason;
import java.util.Objects;

/**
 * Snapshot plus lifecycle reason for runtime publication.
 */
public record PlayerVitalsPublication(
        PlayerVitalsSnapshot snapshot,
        PlayerVitalsLifecycleReason reason
) {
    public PlayerVitalsPublication {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        reason = Objects.requireNonNull(reason, "reason");
    }
}
