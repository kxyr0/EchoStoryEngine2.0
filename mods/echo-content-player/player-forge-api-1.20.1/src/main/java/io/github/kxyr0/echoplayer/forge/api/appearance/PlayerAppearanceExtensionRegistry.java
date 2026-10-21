package io.github.kxyr0.echoplayer.forge.api.appearance;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import java.util.Optional;

/**
 * Public registration boundary for future appearance contributors.
 */
public interface PlayerAppearanceExtensionRegistry {
    void register(PlayerAppearanceContribution contribution);

    default Optional<ResolvedPlayerAppearance> resolve(
            EchoPlayerId playerId,
            PlayerAppearanceSnapshot baseAppearance
    ) {
        return Optional.empty();
    }
}
