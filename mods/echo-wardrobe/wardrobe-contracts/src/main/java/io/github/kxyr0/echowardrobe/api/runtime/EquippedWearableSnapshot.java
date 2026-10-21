package io.github.kxyr0.echowardrobe.api.runtime;

import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.Map;
import java.util.Objects;

/**
 * Derived snapshot of active wearables. Minecraft equipment remains the source of truth.
 */
public record EquippedWearableSnapshot(
        WardrobeProfileId profileId,
        Map<WardrobeSlot, String> equipmentItemIds,
        Map<WardrobeSlot, WearableSelection> selections,
        long catalogRevision
) {
    public EquippedWearableSnapshot {
        profileId = Objects.requireNonNull(profileId, "profileId");
        equipmentItemIds = Map.copyOf(Objects.requireNonNull(equipmentItemIds, "equipmentItemIds"));
        selections = Map.copyOf(Objects.requireNonNull(selections, "selections"));
        if (catalogRevision < 0L) {
            throw new IllegalArgumentException("Catalog revision must not be negative.");
        }
    }

    public static EquippedWearableSnapshot empty(WardrobeProfileId profileId, long catalogRevision) {
        return new EquippedWearableSnapshot(profileId, Map.of(), Map.of(), catalogRevision);
    }
}
