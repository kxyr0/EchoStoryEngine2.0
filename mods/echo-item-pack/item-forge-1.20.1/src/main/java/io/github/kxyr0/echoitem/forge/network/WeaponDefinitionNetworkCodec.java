package io.github.kxyr0.echoitem.forge.network;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.equipment.ItemEquipmentSlot;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.FriendlyByteBuf;

final class WeaponDefinitionNetworkCodec {
    private static final int MAX_STRING = 32767;

    private WeaponDefinitionNetworkCodec() {
    }

    static void writeSnapshot(FriendlyByteBuf buffer, ItemCatalogSnapshot snapshot) {
        buffer.writeLong(snapshot.revision().value());
        buffer.writeVarInt(snapshot.weapons().size());
        snapshot.weapons().values().forEach(definition -> writeWeapon(buffer, definition));
    }

    static ItemCatalogSnapshot readSnapshot(FriendlyByteBuf buffer) {
        ItemDefinitionRevision revision = new ItemDefinitionRevision(buffer.readLong());
        int count = buffer.readVarInt();
        Map<EchoItemId, WeaponDefinition> weapons = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            WeaponDefinition definition = readWeapon(buffer);
            weapons.put(definition.id(), definition);
        }
        return new ItemCatalogSnapshot(revision, weapons);
    }

    private static void writeWeapon(FriendlyByteBuf buffer, WeaponDefinition definition) {
        buffer.writeUtf(definition.id().value());
        writeText(buffer, definition.text());
        writeVisual(buffer, definition.visual());
        writeCombat(buffer, definition.combat());
        writeDurability(buffer, definition.durability());
        buffer.writeUtf(definition.rarity().name());
        buffer.writeBoolean(definition.glint());
        buffer.writeBoolean(definition.fireResistant());
        writeOptionalEquipmentSlot(buffer, definition.equipmentSlot());
        buffer.writeBoolean(definition.hiddenFromCreative());
        writeOptionalInt(buffer, definition.creativeOrder());
        buffer.writeLong(definition.revision().value());
        buffer.writeUtf(definition.sourceFingerprint().sha256());
        buffer.writeBoolean(definition.available());
    }

    private static WeaponDefinition readWeapon(FriendlyByteBuf buffer) {
        EchoItemId id = EchoItemId.parse(buffer.readUtf(MAX_STRING));
        ItemTextDefinition text = readText(buffer);
        ItemVisualDefinition visual = readVisual(buffer);
        WeaponCombatDefinition combat = readCombat(buffer);
        WeaponDurabilityDefinition durability = readDurability(buffer);
        WeaponRarity rarity = WeaponRarity.valueOf(buffer.readUtf(MAX_STRING));
        boolean glint = buffer.readBoolean();
        boolean fireResistant = buffer.readBoolean();
        Optional<ItemEquipmentSlot> equipmentSlot = readOptionalEquipmentSlot(buffer);
        boolean hiddenFromCreative = buffer.readBoolean();
        OptionalInt creativeOrder = readOptionalInt(buffer);
        ItemDefinitionRevision revision = new ItemDefinitionRevision(buffer.readLong());
        ItemSourceFingerprint fingerprint = new ItemSourceFingerprint(buffer.readUtf(MAX_STRING));
        boolean available = buffer.readBoolean();
        return new WeaponDefinition(id, text, visual, combat, durability, rarity, glint, fireResistant, equipmentSlot,
                hiddenFromCreative, creativeOrder, revision, fingerprint, available);
    }

    private static void writeText(FriendlyByteBuf buffer, ItemTextDefinition text) {
        buffer.writeUtf(text.name());
        buffer.writeUtf(text.description());
        buffer.writeVarInt(text.tooltip().size());
        text.tooltip().forEach(buffer::writeUtf);
    }

    private static ItemTextDefinition readText(FriendlyByteBuf buffer) {
        String name = buffer.readUtf(MAX_STRING);
        String description = buffer.readUtf(MAX_STRING);
        int tooltipLines = buffer.readVarInt();
        List<String> tooltip = new ArrayList<>();
        for (int index = 0; index < tooltipLines; index++) {
            tooltip.add(buffer.readUtf(MAX_STRING));
        }
        return new ItemTextDefinition(name, description, tooltip);
    }

    private static void writeVisual(FriendlyByteBuf buffer, ItemVisualDefinition visual) {
        buffer.writeUtf(visual.texture());
        writeOptionalString(buffer, visual.model());
        buffer.writeBoolean(visual.handheld());
        buffer.writeLong(visual.assetRevision());
    }

    private static ItemVisualDefinition readVisual(FriendlyByteBuf buffer) {
        String texture = buffer.readUtf(MAX_STRING);
        Optional<String> model = readOptionalString(buffer);
        boolean handheld = buffer.readBoolean();
        long assetRevision = buffer.readLong();
        return new ItemVisualDefinition(texture, model, handheld, assetRevision);
    }

    private static void writeCombat(FriendlyByteBuf buffer, WeaponCombatDefinition combat) {
        buffer.writeDouble(combat.attackDamage());
        buffer.writeDouble(combat.attacksPerSecond());
        buffer.writeDouble(combat.knockback());
    }

    private static WeaponCombatDefinition readCombat(FriendlyByteBuf buffer) {
        return new WeaponCombatDefinition(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeDurability(FriendlyByteBuf buffer, WeaponDurabilityDefinition durability) {
        buffer.writeVarInt(durability.durability());
        buffer.writeVarInt(durability.enchantability());
        writeOptionalString(buffer, durability.repairIngredient());
        buffer.writeBoolean(durability.unbreakable());
    }

    private static WeaponDurabilityDefinition readDurability(FriendlyByteBuf buffer) {
        int durability = buffer.readVarInt();
        int enchantability = buffer.readVarInt();
        Optional<String> repairIngredient = readOptionalString(buffer);
        boolean unbreakable = buffer.readBoolean();
        return new WeaponDurabilityDefinition(durability, enchantability, repairIngredient, unbreakable);
    }

    private static void writeOptionalString(FriendlyByteBuf buffer, Optional<String> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeUtf);
    }

    private static Optional<String> readOptionalString(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readUtf(MAX_STRING)) : Optional.empty();
    }

    private static void writeOptionalInt(FriendlyByteBuf buffer, OptionalInt value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeVarInt);
    }

    private static OptionalInt readOptionalInt(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? OptionalInt.of(buffer.readVarInt()) : OptionalInt.empty();
    }

    private static void writeOptionalEquipmentSlot(FriendlyByteBuf buffer, Optional<ItemEquipmentSlot> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(slot -> buffer.writeUtf(slot.name()));
    }

    private static Optional<ItemEquipmentSlot> readOptionalEquipmentSlot(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(ItemEquipmentSlot.valueOf(buffer.readUtf(MAX_STRING)))
                : Optional.empty();
    }
}
