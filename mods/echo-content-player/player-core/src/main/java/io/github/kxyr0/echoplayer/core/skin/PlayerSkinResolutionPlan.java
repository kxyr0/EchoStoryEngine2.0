package io.github.kxyr0.echoplayer.core.skin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded fallback plan for resource skin publication.
 */
public record PlayerSkinResolutionPlan(
        Optional<PlayerAppearanceSnapshot> accepted,
        Optional<PlayerAppearanceSnapshot> previousKnownGood,
        boolean accountFallback,
        String diagnostic
) {
    public PlayerSkinResolutionPlan {
        accepted = Objects.requireNonNull(accepted, "accepted");
        previousKnownGood = Objects.requireNonNull(previousKnownGood, "previousKnownGood");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public static PlayerSkinResolutionPlan accepted(PlayerAppearanceSnapshot snapshot) {
        return new PlayerSkinResolutionPlan(Optional.of(snapshot), Optional.empty(), false, "");
    }

    public static PlayerSkinResolutionPlan fallback(Optional<PlayerAppearanceSnapshot> previous, String diagnostic) {
        return new PlayerSkinResolutionPlan(Optional.empty(), previous, previous.isEmpty(), diagnostic);
    }
}
