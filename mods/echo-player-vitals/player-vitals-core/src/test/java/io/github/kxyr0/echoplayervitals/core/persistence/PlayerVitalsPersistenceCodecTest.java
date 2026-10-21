package io.github.kxyr0.echoplayervitals.core.persistence;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.fatigue.PlayerFatigueSnapshotFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerVitalsPersistenceCodecTest {
    @Test
    void roundTripKeepsDurableFatigueAndRevision() {
        PlayerVitalsPersistenceCodec codec = new PlayerVitalsPersistenceCodec();
        PlayerVitalsSnapshot snapshot = snapshot(new PlayerFatigue(75, 100), new PlayerVitalsRevision(7L));

        PlayerVitalsSnapshot decoded = codec.decode(codec.encode(PlayerVitalsData.fromSnapshot(snapshot)))
                .toSnapshot(PlayerVitalsConfig.defaults());

        assertEquals(snapshot.playerId(), decoded.playerId());
        assertEquals(75, decoded.fatigue().fatigue().current());
        assertEquals(7L, decoded.revision().value());
    }

    private PlayerVitalsSnapshot snapshot(PlayerFatigue fatigue, PlayerVitalsRevision revision) {
        return new PlayerVitalsSnapshot(PlayerVitalsId.fromAccountUuid(UUID.randomUUID()),
                new PlayerFatigueSnapshotFactory().snapshot(fatigue, PlayerVitalsConfig.defaults(), revision)
                        .value().orElseThrow(), new PlayerHealthSnapshot(20.0F, 20.0F, true),
                PlayerHungerSnapshot.neutral(), revision);
    }
}
