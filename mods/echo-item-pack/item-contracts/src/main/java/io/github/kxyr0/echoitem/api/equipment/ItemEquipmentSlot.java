package io.github.kxyr0.echoitem.api.equipment;

import java.util.Locale;
import java.util.Objects;

/**
 * Neutral equipment slots that do not depend on a specific Minecraft loader.
 */
public enum ItemEquipmentSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET;

    public static ItemEquipmentSlot parse(String value) {
        Objects.requireNonNull(value, "value");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("equipmentSlot must be HEAD, CHEST, LEGS, or FEET.", ex);
        }
    }
}
