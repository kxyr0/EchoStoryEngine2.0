package io.github.kxyr0.echoitem.forge.item;

import com.google.common.collect.Multimap;
import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.equipment.ItemEquipmentSlot;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Generic Forge item whose behavior is resolved from the active catalog by stable EchoItemId.
 */
public final class EchoWeaponItem extends Item {
    private final EchoItemId id;
    private final WeaponDefinitionCatalog catalog;
    private final WeaponAttributeAssembler attributes = new WeaponAttributeAssembler();

    public EchoWeaponItem(EchoItemId id, WeaponDefinitionCatalog catalog) {
        super(new Item.Properties().stacksTo(1).durability(1));
        this.id = Objects.requireNonNull(id, "id");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public EchoItemId echoItemId() {
        return id;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition().text().name());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WeaponDefinition definition = definition();
        if (!definition.text().description().isBlank()) {
            tooltip.add(Component.literal(definition.text().description()).withStyle(ChatFormatting.GRAY));
        }
        definition.text().tooltip().forEach(line -> tooltip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes.assemble(definition()) : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        return definition().equipmentSlot()
                .map(slot -> equipmentSlot(slot) == armorType)
                .orElseGet(() -> super.canEquip(stack, armorType, entity));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return definition().durability().unbreakable() ? 0 : definition().durability().durability();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return definition().available() && !definition().durability().unbreakable();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return definition().available() && definition().durability().enchantability() > 0;
    }

    @Override
    public int getEnchantmentValue() {
        return definition().durability().enchantability();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return definition().glint() || super.isFoil(stack);
    }

    @Override
    public boolean isFireResistant() {
        return definition().fireResistant();
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return switch (definition().rarity()) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case EPIC -> Rarity.EPIC;
        };
    }

    private WeaponDefinition definition() {
        return catalog.current(id);
    }

    private EquipmentSlot equipmentSlot(ItemEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
        };
    }
}
