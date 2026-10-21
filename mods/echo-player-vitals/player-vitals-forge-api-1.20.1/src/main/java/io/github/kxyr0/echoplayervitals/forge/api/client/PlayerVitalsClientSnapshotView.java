package io.github.kxyr0.echoplayervitals.forge.api.client;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.Optional;

/**
 * Client-local view of latest synchronized Vitals snapshots.
 */
public interface PlayerVitalsClientSnapshotView {
    Optional<PlayerVitalsSnapshot> local();

    Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId);
}
