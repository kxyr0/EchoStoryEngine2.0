package io.github.kxyr0.echowardrobe.core.equipment;

import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Derived view of current vanilla equipment item IDs.
 */
public record EquipmentItemSnapshot(Map<WardrobeEquipmentSlot, String> itemIds) {
    public EquipmentItemSnapshot {
        itemIds = Map.copyOf(Objects.requireNonNull(itemIds, "itemIds"));
    }

    public static EquipmentItemSnapshot empty() {
        return new EquipmentItemSnapshot(Map.of());
    }

    public Optional<String> itemId(WardrobeEquipmentSlot slot) {
        return Optional.ofNullable(itemIds.get(Objects.requireNonNull(slot, "slot")));
    }
}
