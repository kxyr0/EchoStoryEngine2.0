package io.github.kxyr0.echoplayer.forge.api.appearance;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.PlayerAppearanceContributionId;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import java.util.Optional;

/**
 * Neutral extension point for equipment- or state-derived player appearance.
 */
public interface PlayerAppearanceContribution {
    PlayerAppearanceContributionId id();

    Optional<ResolvedPlayerAppearance> resolve(EchoPlayerId playerId, PlayerAppearanceSnapshot baseAppearance);
}
