package io.github.kxyr0.echoplayervitals.core.mutation;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.fatigue.PlayerFatigueSnapshotFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerFatigueMutationTest {
    private final PlayerFatigueMutation mutation = new PlayerFatigueMutation();

    @Test
    void mutationClampsToFatigueRange() {
        PlayerFatigue fatigue = new PlayerFatigue(90, 100);

        assertEquals(100, mutation.add(fatigue, 50).fatigue().current());
        assertEquals(0, mutation.reduce(fatigue, 200).fatigue().current());
        assertEquals(100, mutation.set(fatigue, 500).fatigue().current());
    }

    @Test
    void restoreFullyReturnsRestedFatigue() {
        assertEquals(0, mutation.restoreFully(new PlayerFatigue(75, 100)).fatigue().current());
    }

    @Test
    void ledgerReplaysPreviousMutationResult() {
        PlayerVitalsMutationLedger ledger = new PlayerVitalsMutationLedger();
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(UUID.randomUUID());
        PlayerVitalsMutationId mutationId = new PlayerVitalsMutationId("test:fatigue:add");
        PlayerVitalsSnapshot snapshot = snapshot(playerId, new PlayerFatigue(25, 100));

        ledger.remember(playerId, mutationId, snapshot);

        assertTrue(ledger.replay(playerId, mutationId).isPresent());
        assertEquals(25, ledger.replay(playerId, mutationId).orElseThrow().fatigue().fatigue().current());
    }

    private PlayerVitalsSnapshot snapshot(PlayerVitalsId playerId, PlayerFatigue fatigue) {
        PlayerVitalsRevision revision = PlayerVitalsRevision.initial();
        return new PlayerVitalsSnapshot(playerId, new PlayerFatigueSnapshotFactory()
                .snapshot(fatigue, PlayerVitalsConfig.defaults(), revision).value().orElseThrow(),
                new PlayerHealthSnapshot(20.0F, 20.0F, true), PlayerHungerSnapshot.neutral(), revision);
    }
}
