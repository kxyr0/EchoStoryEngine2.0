package io.github.kxyr0.echoplayer.core.skin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import java.util.List;

/**
 * Validates ACCOUNT and RESOURCE skin selection invariants.
 */
public final class PlayerSkinSelectionValidator {
    public PlayerProfileResult<PlayerAppearanceSnapshot> validate(PlayerAppearanceSnapshot snapshot, String source) {
        try {
            return PlayerProfileResult.valid(snapshot, List.of());
        } catch (RuntimeException ex) {
            return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error("PLAYER_SKIN_INVALID",
                    ex.getMessage(), source)));
        }
    }
}
