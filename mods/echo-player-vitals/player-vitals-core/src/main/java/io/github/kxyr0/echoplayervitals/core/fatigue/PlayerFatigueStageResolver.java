package io.github.kxyr0.echoplayervitals.core.fatigue;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsDiagnostic;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsResult;
import io.github.kxyr0.echoplayervitals.core.config.PlayerFatigueStageRule;
import io.github.kxyr0.echoplayervitals.core.diagnostic.PlayerVitalsDiagnosticCodes;
import java.util.List;
import java.util.Objects;

/**
 * Resolves the single fatigue stage rule matching the current reserve.
 */
public final class PlayerFatigueStageResolver {
    public PlayerVitalsResult<PlayerFatigueStageRule> resolve(PlayerFatigue fatigue,
                                                              List<PlayerFatigueStageRule> rules) {
        Objects.requireNonNull(fatigue, "fatigue");
        double reserve = fatigue.reserve().normalized();
        List<PlayerFatigueStageRule> matches = Objects.requireNonNull(rules, "rules").stream()
                .filter(rule -> rule.matches(reserve))
                .toList();
        if (matches.size() == 1) {
            return PlayerVitalsResult.valid(matches.get(0), List.of());
        }
        if (matches.isEmpty()) {
            return PlayerVitalsResult.invalid(List.of(PlayerVitalsDiagnostic.error(
                    PlayerVitalsDiagnosticCodes.STAGE_MISSING,
                    "No fatigue stage covers reserve " + reserve + ".", "fatigue-stage")));
        }
        return PlayerVitalsResult.invalid(List.of(PlayerVitalsDiagnostic.error(
                PlayerVitalsDiagnosticCodes.STAGE_AMBIGUOUS,
                "More than one fatigue stage covers reserve " + reserve + ".", "fatigue-stage")));
    }
}
