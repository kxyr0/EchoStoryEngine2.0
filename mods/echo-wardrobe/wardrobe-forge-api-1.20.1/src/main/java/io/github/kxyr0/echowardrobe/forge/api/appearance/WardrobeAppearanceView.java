package io.github.kxyr0.echowardrobe.forge.api.appearance;

import io.github.kxyr0.echowardrobe.api.contribution.WardrobeAppearanceContribution;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.runtime.EquippedWearableSnapshot;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-only public resolved wardrobe appearance view.
 */
public interface WardrobeAppearanceView {
    Optional<EquippedWearableSnapshot> equippedWearables(UUID playerUuid);

    Optional<WardrobeAppearanceContribution> wardrobeAppearance(UUID playerUuid);

    Optional<ResolvedOutfit> resolvedOutfit(UUID playerUuid);
}
