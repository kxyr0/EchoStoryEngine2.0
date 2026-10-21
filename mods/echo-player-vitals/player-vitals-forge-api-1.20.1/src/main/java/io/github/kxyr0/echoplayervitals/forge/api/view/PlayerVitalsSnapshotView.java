package io.github.kxyr0.echoplayervitals.forge.api.view;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;

/**
 * Segregated read-only view of a full Vitals snapshot.
 */
public interface PlayerVitalsSnapshotView {
    PlayerVitalsId playerId();

    PlayerFatigueSnapshot fatigue();

    PlayerHealthSnapshot health();

    PlayerHungerSnapshot hunger();

    PlayerVitalsRevision revision();
}
