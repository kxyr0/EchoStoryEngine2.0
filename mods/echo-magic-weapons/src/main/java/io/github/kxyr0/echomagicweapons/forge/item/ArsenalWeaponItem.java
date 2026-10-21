package io.github.kxyr0.echomagicweapons.forge.item;

import io.github.kxyr0.echomagicweapons.forge.EchoMagicWeaponsMod;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class ArsenalWeaponItem extends SwordItem {
    private final ArsenalWeaponKind kind;

    ArsenalWeaponItem(ArsenalWeaponKind kind) {
        super(Tiers.IRON, kind.damage(), kind.speed(), new Properties().stacksTo(1).fireResistant());
        this.kind = kind;
    }

    public ArsenalWeaponKind kind() { return kind; }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.consume(stack);
        if (kind == ArsenalWeaponKind.FIRE || kind == ArsenalWeaponKind.WATER) {
            EchoMagicWeaponsMod.runtime().specialUse(serverPlayer, kind);
        } else {
            player.startUsingItem(hand);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof ServerPlayer player) {
            EchoMagicWeaponsMod.runtime().releaseUsing(player, kind, getUseDuration(stack) - timeLeft);
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) EchoMagicWeaponsMod.runtime().weaponSwing(player, kind);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.echo_magic_weapons.temporary").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.echo_magic_weapons." + kind.id() + ".hint")
                .withStyle(ChatFormatting.AQUA));
    }
}
