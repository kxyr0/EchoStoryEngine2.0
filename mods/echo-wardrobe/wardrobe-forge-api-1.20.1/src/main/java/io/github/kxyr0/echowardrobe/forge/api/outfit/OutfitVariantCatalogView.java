package io.github.kxyr0.echowardrobe.forge.api.outfit;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import java.util.List;
import java.util.Optional;

/**
 * Query-only exact outfit variant view.
 */
public interface OutfitVariantCatalogView {
    int outfitVariantCount();

    List<OutfitVariantDefinition> outfitVariants();

    Optional<OutfitVariantDefinition> outfitVariant(OutfitVariantId id);

    Optional<OutfitVariantDefinition> exactOutfit(OutfitKey key);

    List<OutfitVariantDefinition> outfitVariants(WardrobeProfileId profileId);
}
