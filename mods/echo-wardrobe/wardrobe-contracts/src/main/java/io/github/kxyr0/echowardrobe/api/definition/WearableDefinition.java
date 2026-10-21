package io.github.kxyr0.echowardrobe.api.definition;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitOverlayContribution;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WearableDefinitionRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable item-to-wardrobe-slot definition produced by the EchoScript binder.
 */
public record WearableDefinition(
        WearableId id,
        WardrobeProfileId profileId,
        WardrobeSlot slot,
        String collection,
        WearableItemBinding itemBinding,
        Optional<WearableModelContribution> model,
        Optional<WearablePortraitOverlayContribution> portraitOverlay,
        WearableDefinitionStatus status,
        WearableDefinitionRevision revision,
        WearableSourceFingerprint sourceFingerprint
) {
    public WearableDefinition {
        id = Objects.requireNonNull(id, "id");
        profileId = Objects.requireNonNull(profileId, "profileId");
        slot = Objects.requireNonNull(slot, "slot");
        collection = Objects.requireNonNull(collection, "collection");
        itemBinding = Objects.requireNonNull(itemBinding, "itemBinding");
        model = Objects.requireNonNull(model, "model");
        portraitOverlay = Objects.requireNonNull(portraitOverlay, "portraitOverlay");
        status = Objects.requireNonNull(status, "status");
        revision = Objects.requireNonNull(revision, "revision");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (slot.equipmentSlot() != itemBinding.equipmentSlot()) {
            throw new IllegalArgumentException("Wardrobe slot and equipment slot do not match.");
        }
    }

    public boolean active() {
        return status == WearableDefinitionStatus.ACTIVE;
    }
}
