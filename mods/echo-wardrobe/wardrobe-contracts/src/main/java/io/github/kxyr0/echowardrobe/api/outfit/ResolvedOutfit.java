package io.github.kxyr0.echowardrobe.api.outfit;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact resolved player wardrobe appearance.
 */
public record ResolvedOutfit(
        WardrobeProfileId profileId,
        OutfitKey outfitKey,
        Optional<OutfitVariantId> outfitVariantId,
        String worldSkin,
        String faceSkin,
        List<WearableModelContribution> models,
        ResolvedOutfitFallback fallback,
        WardrobeCatalogRevision catalogRevision,
        long appearanceRevision
) {
    public ResolvedOutfit {
        profileId = Objects.requireNonNull(profileId, "profileId");
        outfitKey = Objects.requireNonNull(outfitKey, "outfitKey");
        outfitVariantId = Objects.requireNonNull(outfitVariantId, "outfitVariantId");
        worldSkin = Objects.requireNonNull(worldSkin, "worldSkin");
        faceSkin = Objects.requireNonNull(faceSkin, "faceSkin");
        models = List.copyOf(Objects.requireNonNull(models, "models"));
        fallback = Objects.requireNonNull(fallback, "fallback");
        catalogRevision = Objects.requireNonNull(catalogRevision, "catalogRevision");
        if (appearanceRevision < 0L) {
            throw new IllegalArgumentException("Appearance revision cannot be negative.");
        }
    }
}
