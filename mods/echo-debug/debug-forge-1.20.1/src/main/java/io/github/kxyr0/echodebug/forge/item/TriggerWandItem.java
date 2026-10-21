package io.github.kxyr0.echodebug.forge.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class TriggerWandItem extends Item {
    public TriggerWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echo_debug.trigger_wand.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_debug.trigger_wand.line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_debug.trigger_wand.line3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_debug.trigger_wand.line4").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_debug.trigger_wand.line5").withStyle(ChatFormatting.GRAY));
    }
}
