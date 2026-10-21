package io.github.kxyr0.echoplayer.forge.appearance;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceContribution;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceExtensionRegistry;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public registration point for future appearance contributors.
 */
public final class PlayerAppearanceExtensions implements PlayerAppearanceExtensionRegistry {
    private final List<PlayerAppearanceContribution> contributions = new ArrayList<>();

    @Override
    public synchronized void register(PlayerAppearanceContribution contribution) {
        contributions.add(Objects.requireNonNull(contribution, "contribution"));
    }

    public synchronized List<PlayerAppearanceContribution> snapshot() {
        return List.copyOf(contributions);
    }

    @Override
    public Optional<ResolvedPlayerAppearance> resolve(EchoPlayerId playerId, PlayerAppearanceSnapshot baseAppearance) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(baseAppearance, "baseAppearance");
        for (PlayerAppearanceContribution contribution : snapshot()) {
            Optional<ResolvedPlayerAppearance> resolved = contribution.resolve(playerId, baseAppearance);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }
}
