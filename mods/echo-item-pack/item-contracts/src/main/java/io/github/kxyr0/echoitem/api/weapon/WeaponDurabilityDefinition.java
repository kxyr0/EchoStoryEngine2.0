package io.github.kxyr0.echoitem.api.weapon;

import java.util.Objects;
import java.util.Optional;

/**
 * Runtime durability, repair, and enchanting values.
 */
public record WeaponDurabilityDefinition(
        int durability,
        int enchantability,
        Optional<String> repairIngredient,
        boolean unbreakable
) {
    public WeaponDurabilityDefinition {
        if ((!unbreakable && durability <= 0) || durability < 0 || durability > 100000) {
            throw new IllegalArgumentException("Durability must be positive and bounded.");
        }
        if (enchantability < 0 || enchantability > 100) {
            throw new IllegalArgumentException("Enchantability must be bounded.");
        }
        repairIngredient = Objects.requireNonNull(repairIngredient, "repairIngredient");
    }
}
