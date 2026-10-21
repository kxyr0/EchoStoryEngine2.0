package io.github.kxyr0.echotrigger.forge.provider.payload;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ForgeIds {
    private ForgeIds() {
    }

    public static TriggerOwnerId owner(Player player) {
        return new TriggerOwnerId("player:" + player.getUUID());
    }

    public static String item(ItemStack stack) {
        return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String itemTags(ItemStack stack) {
        return stack.getTags().map(TagKey::location).map(ResourceLocation::toString)
                .collect(Collectors.joining(","));
    }

    public static String block(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static String blockTags(BlockState state) {
        return state.getTags().map(TagKey::location).map(ResourceLocation::toString)
                .collect(Collectors.joining(","));
    }

    public static String effect(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
    }

    public static String dimension(Level level) {
        return level.dimension().location().toString();
    }

    public static String hand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? "OFF_HAND" : "MAIN_HAND";
    }

    public static String gameMode(GameType gameType) {
        return gameType.getName().toUpperCase(Locale.ROOT);
    }

    public static String weather(Level level) {
        if (level.isThundering()) {
            return "THUNDER";
        }
        return level.isRaining() ? "RAIN" : "CLEAR";
    }

    public static String damageSource(DamageSource source) {
        return "minecraft:" + source.getMsgId();
    }

    public static String position(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static Vector3 position(Player player) {
        return new Vector3(player.getX(), player.getY(), player.getZ());
    }
}
