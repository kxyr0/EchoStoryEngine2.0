package io.github.kxyr0.echowardrobe.forge.api.player;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-only player exact outfit state.
 */
public interface PlayerWardrobeView {
    Optional<OutfitKey> outfitKey(UUID playerUuid);

    long appearanceRevision(UUID playerUuid);
}
