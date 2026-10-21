package io.github.kxyr0.echoplayervitals.core.persistence;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.fatigue.PlayerFatigueSnapshotFactory;

/**
 * Versioned persistence DTO. It is not part of the public API.
 */
public record PlayerVitalsData(
        String playerId,
        int fatigueCurrent,
        int fatigueMaximum,
        long revision
) {
    public static PlayerVitalsData fromSnapshot(PlayerVitalsSnapshot snapshot) {
        return new PlayerVitalsData(snapshot.playerId().value(), snapshot.fatigue().fatigue().current(),
                snapshot.fatigue().fatigue().maximum(), snapshot.revision().value());
    }

    public PlayerVitalsSnapshot toSnapshot(PlayerVitalsConfig config) {
        PlayerVitalsRevision snapshotRevision = new PlayerVitalsRevision(revision);
        PlayerFatigue fatigue = new PlayerFatigue(fatigueCurrent, fatigueMaximum);
        return new PlayerVitalsSnapshot(PlayerVitalsId.parse(playerId),
                new PlayerFatigueSnapshotFactory().snapshot(fatigue, config, snapshotRevision).value().orElseThrow(),
                new PlayerHealthSnapshot(20.0F, 20.0F, true), PlayerHungerSnapshot.neutral(), snapshotRevision);
    }
}
