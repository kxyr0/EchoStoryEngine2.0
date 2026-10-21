package io.github.kxyr0.echobloodmagic.forge.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

final class BloodRapierItem extends SwordItem {
    BloodRapierItem() { super(Tiers.IRON, 1, -1.8F, new Properties().stacksTo(1).fireResistant()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.echo_blood_magic.blood_rapier.temporary")
                .withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.translatable("item.echo_blood_magic.blood_rapier.particle_blade")
                .withStyle(ChatFormatting.RED));
    }
}
