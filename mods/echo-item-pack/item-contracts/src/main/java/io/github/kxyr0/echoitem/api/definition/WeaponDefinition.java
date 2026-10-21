package io.github.kxyr0.echoitem.api.definition;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.equipment.ItemEquipmentSlot;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Immutable melee weapon definition.
 */
public record WeaponDefinition(
        EchoItemId id,
        ItemTextDefinition text,
        ItemVisualDefinition visual,
        WeaponCombatDefinition combat,
        WeaponDurabilityDefinition durability,
        WeaponRarity rarity,
        boolean glint,
        boolean fireResistant,
        Optional<ItemEquipmentSlot> equipmentSlot,
        boolean hiddenFromCreative,
        OptionalInt creativeOrder,
        ItemDefinitionRevision revision,
        ItemSourceFingerprint sourceFingerprint,
        boolean available
) implements ItemDefinition {
    public WeaponDefinition {
        id = Objects.requireNonNull(id, "id");
        text = Objects.requireNonNull(text, "text");
        visual = Objects.requireNonNull(visual, "visual");
        combat = Objects.requireNonNull(combat, "combat");
        durability = Objects.requireNonNull(durability, "durability");
        rarity = Objects.requireNonNull(rarity, "rarity");
        equipmentSlot = Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        creativeOrder = Objects.requireNonNull(creativeOrder, "creativeOrder");
        revision = Objects.requireNonNull(revision, "revision");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
    }

    @Override
    public ItemKindId kind() {
        return ItemKindId.WEAPON;
    }
}
