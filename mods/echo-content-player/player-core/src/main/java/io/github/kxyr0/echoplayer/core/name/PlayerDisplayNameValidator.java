package io.github.kxyr0.echoplayer.core.name;

import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import java.util.List;

/**
 * Deterministic plain-text Echo display name validator.
 */
public final class PlayerDisplayNameValidator {
    public PlayerProfileResult<PlayerDisplayName> validate(String rawName, String source) {
        try {
            return PlayerProfileResult.valid(PlayerDisplayName.of(rawName), List.of());
        } catch (RuntimeException ex) {
            return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error(
                    "PLAYER_NAME_INVALID", ex.getMessage(), source)));
        }
    }
}
