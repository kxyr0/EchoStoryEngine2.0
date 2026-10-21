package io.github.kxyr0.echowardrobe.api.outfit;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.OutfitVariantRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import java.util.Objects;

/**
 * Exact outfit variant resolved by OutfitKey lookup.
 */
public record OutfitVariantDefinition(
        OutfitVariantId id,
        WardrobeProfileId profileId,
        OutfitKey key,
        String worldSkin,
        String faceSkin,
        OutfitVariantStatus status,
        OutfitVariantRevision revision,
        WearableSourceFingerprint sourceFingerprint
) {
    public OutfitVariantDefinition {
        id = Objects.requireNonNull(id, "id");
        profileId = Objects.requireNonNull(profileId, "profileId");
        key = Objects.requireNonNull(key, "key");
        worldSkin = WardrobeResourceId.require(worldSkin, "World skin");
        faceSkin = WardrobeResourceId.require(faceSkin, "Face skin");
        status = Objects.requireNonNull(status, "status");
        revision = Objects.requireNonNull(revision, "revision");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (!profileId.equals(key.profileId())) {
            throw new IllegalArgumentException("Outfit profile must match key profile.");
        }
    }
}
