package io.github.kxyr0.echoplayervitals.core.fatigue;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsResult;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.core.config.PlayerFatigueStageRule;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import java.util.Objects;

/**
 * Builds immutable fatigue snapshots from raw fatigue and bound config.
 */
public final class PlayerFatigueSnapshotFactory {
    private final PlayerFatigueStageResolver resolver = new PlayerFatigueStageResolver();

    public PlayerVitalsResult<PlayerFatigueSnapshot> snapshot(PlayerFatigue fatigue,
                                                              PlayerVitalsConfig config,
                                                              PlayerVitalsRevision revision) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(revision, "revision");
        PlayerVitalsResult<PlayerFatigueStageRule> result = resolver.resolve(fatigue, config.stageRules());
        return result.value()
                .map(rule -> PlayerVitalsResult.valid(toSnapshot(fatigue, rule, revision), result.diagnostics()))
                .orElseGet(() -> PlayerVitalsResult.invalid(result.diagnostics()));
    }

    private PlayerFatigueSnapshot toSnapshot(PlayerFatigue fatigue, PlayerFatigueStageRule rule,
                                             PlayerVitalsRevision revision) {
        return new PlayerFatigueSnapshot(fatigue, fatigue.reserve(), rule.stage(), rule.movementMultiplier(),
                rule.nauseaIntensity(), revision);
    }
}
