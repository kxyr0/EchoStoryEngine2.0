package io.github.kxyr0.echoplayervitals.core.config;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bound vitals configuration used by the runtime.
 */
public record PlayerVitalsConfig(int fatigueMaximum, List<PlayerFatigueStageRule> stageRules) {
    public PlayerVitalsConfig {
        if (fatigueMaximum <= 0) {
            throw new IllegalArgumentException("Fatigue maximum must be positive.");
        }
        stageRules = List.copyOf(Objects.requireNonNull(stageRules, "stageRules"));
        if (stageRules.isEmpty()) {
            throw new IllegalArgumentException("At least one fatigue stage rule is required.");
        }
    }

    public static PlayerVitalsConfig defaults() {
        return new PlayerVitalsConfig(100, List.of(
                new PlayerFatigueStageRule(PlayerFatigueStage.RESTED, Optional.empty(),
                        Optional.of(0.50D), 1.0D, 0.0D),
                new PlayerFatigueStageRule(PlayerFatigueStage.TIRED, Optional.of(0.50D),
                        Optional.of(0.25D), 0.95D, 0.0D),
                new PlayerFatigueStageRule(PlayerFatigueStage.EXHAUSTED, Optional.of(0.25D),
                        Optional.empty(), 0.85D, 0.35D)
        ));
    }
}
