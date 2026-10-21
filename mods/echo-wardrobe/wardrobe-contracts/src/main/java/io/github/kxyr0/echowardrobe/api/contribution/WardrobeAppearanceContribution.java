package io.github.kxyr0.echowardrobe.api.contribution;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-authored exact outfit contribution sent to player appearance consumers.
 */
public record WardrobeAppearanceContribution(
        WardrobeProfileId profileId,
        OutfitKey outfitKey,
        Optional<OutfitVariantId> outfitVariantId,
        String worldSkin,
        String faceSkin,
        List<WearableModelContribution> modelContributions,
        WardrobeCatalogRevision catalogRevision,
        long appearanceRevision
) {
    public WardrobeAppearanceContribution {
        profileId = Objects.requireNonNull(profileId, "profileId");
        outfitKey = Objects.requireNonNull(outfitKey, "outfitKey");
        outfitVariantId = Objects.requireNonNull(outfitVariantId, "outfitVariantId");
        worldSkin = Objects.requireNonNull(worldSkin, "worldSkin");
        faceSkin = Objects.requireNonNull(faceSkin, "faceSkin");
        modelContributions = List.copyOf(Objects.requireNonNull(modelContributions, "modelContributions"));
        catalogRevision = Objects.requireNonNull(catalogRevision, "catalogRevision");
        if (appearanceRevision < 0L) {
            throw new IllegalArgumentException("Appearance revision cannot be negative.");
        }
    }

    public String renderKey() {
        return outfitKey.serialize() + "|" + worldSkin + "|" + faceSkin + "|" + appearanceRevision;
    }
}
