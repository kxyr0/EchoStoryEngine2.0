package io.github.kxyr0.echoplayervitals.api.snapshot;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import java.util.Objects;

/**
 * Full immutable player vitals snapshot.
 */
public record PlayerVitalsSnapshot(
        PlayerVitalsId playerId,
        PlayerFatigueSnapshot fatigue,
        PlayerHealthSnapshot health,
        PlayerHungerSnapshot hunger,
        PlayerVitalsRevision revision
) {
    public PlayerVitalsSnapshot {
        playerId = Objects.requireNonNull(playerId, "playerId");
        fatigue = Objects.requireNonNull(fatigue, "fatigue");
        health = Objects.requireNonNull(health, "health");
        hunger = Objects.requireNonNull(hunger, "hunger");
        revision = Objects.requireNonNull(revision, "revision");
    }
}
