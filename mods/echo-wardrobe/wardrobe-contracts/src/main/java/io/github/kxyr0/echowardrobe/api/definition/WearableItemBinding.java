package io.github.kxyr0.echowardrobe.api.definition;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import java.util.Objects;

/**
 * Mapping from a wearable definition to a Minecraft item registry ID.
 */
public record WearableItemBinding(String itemId, WardrobeEquipmentSlot equipmentSlot) {
    public WearableItemBinding {
        itemId = WardrobeResourceId.require(itemId, "Item ID");
        equipmentSlot = Objects.requireNonNull(equipmentSlot, "equipmentSlot");
    }
}
