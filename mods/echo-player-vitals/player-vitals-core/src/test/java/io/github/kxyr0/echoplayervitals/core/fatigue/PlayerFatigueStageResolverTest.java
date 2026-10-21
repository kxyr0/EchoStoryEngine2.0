package io.github.kxyr0.echoplayervitals.core.fatigue;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerFatigueStageResolverTest {
    private final PlayerFatigueSnapshotFactory factory = new PlayerFatigueSnapshotFactory();

    @Test
    void reserveIsInvertedFromRawFatigue() {
        var snapshot = snapshot(75);

        assertEquals(25, snapshot.reserve().current());
        assertEquals(0.25D, snapshot.reserve().normalized());
    }

    @Test
    void exactBoundariesResolveToExpectedStages() {
        assertEquals(PlayerFatigueStage.RESTED, snapshot(0).stage());
        assertEquals(PlayerFatigueStage.TIRED, snapshot(50).stage());
        assertEquals(PlayerFatigueStage.EXHAUSTED, snapshot(75).stage());
        assertEquals(PlayerFatigueStage.EXHAUSTED, snapshot(100).stage());
    }

    @Test
    void movementAndNauseaComeFromStageRules() {
        assertEquals(1.0D, snapshot(0).movementMultiplier());
        assertEquals(0.95D, snapshot(50).movementMultiplier());
        assertEquals(0.85D, snapshot(100).movementMultiplier());
        assertEquals(0.35D, snapshot(100).nauseaIntensity());
    }

    private io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot snapshot(int fatigue) {
        return factory.snapshot(new PlayerFatigue(fatigue, 100), PlayerVitalsConfig.defaults(),
                PlayerVitalsRevision.initial()).value().orElseThrow();
    }
}
