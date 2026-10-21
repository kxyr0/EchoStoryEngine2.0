package io.github.kxyr0.echowardrobe.api.slot;

/**
 * W1 wardrobe slots mapped onto vanilla equipment slots.
 */
public enum WardrobeSlot {
    FACE(WardrobeEquipmentSlot.HEAD),
    TOP(WardrobeEquipmentSlot.CHEST),
    BOTTOM(WardrobeEquipmentSlot.LEGS),
    FEET(WardrobeEquipmentSlot.FEET);

    private final WardrobeEquipmentSlot equipmentSlot;

    WardrobeSlot(WardrobeEquipmentSlot equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    public WardrobeEquipmentSlot equipmentSlot() {
        return equipmentSlot;
    }
}
