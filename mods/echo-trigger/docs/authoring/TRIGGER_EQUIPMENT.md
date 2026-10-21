# Руки и экипировка

Hand predicates:

- `Holding(ItemIdOrTag)`
- `HoldingMainHand(ItemIdOrTag)`
- `HoldingOffHand(ItemIdOrTag)`

Hand events:

- `HeldItemChanged()`
- `MainHandChanged(from: OldSword, to: NewSword)`
- `OffHandChanged(to: Shield)`

Armor slots: `HEAD`, `CHEST`, `LEGS`, `FEET`.

Equipment predicates:

- `Wearing(slot, ItemIdOrTag)`
- `HasAnyArmor()`
- `HasFullArmorSet("#namespace:armor_tag")`

Equipment events:

- `EquipmentChanged(slot)`
- `EquipmentEquipped(slot, ItemIdOrTag)`
- `EquipmentUnequipped(slot, ItemIdOrTag)`

Events are transition-only. Recovery never synthesizes equip, unequip or hand-change events.
