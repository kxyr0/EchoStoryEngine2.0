package io.github.kxyr0.echofiremagic.forge.blacksmith;

import io.github.kxyr0.echofiremagic.forge.FireMagicIds;
import io.github.kxyr0.echofiremagic.forge.api.event.BlacksmithHeatBlockEvent;
import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class BlacksmithHandRuntime {
    private static final double BLOCK_REACH = 5.0D;

    public void onMagicCast(MagicLifecycleEvent event) {
        if (!FireMagicIds.BLACKSMITH_HAND.equals(event.spellId())) return;
        event.markHandled();
        var server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null
                : server.getPlayerList().getPlayer(event.player().uuid());
        if (player == null) return;
        Optional<BlacksmithHeatJob.Target> target = findTarget(player);
        if (target.isEmpty()) {
            message(player, "message.echo_fire_magic.blacksmith.no_target");
            return;
        }
        BlacksmithHeatJob.Target selected = target.orElseThrow();
        boolean completed = complete(player, selected);
        message(player, completed ? "message.echo_fire_magic.blacksmith.complete"
                : "message.echo_fire_magic.blacksmith.interrupted");
        if (!completed) return;
        particles(player, selected);
        player.serverLevel().playSound(null, targetPosition(player, selected), SoundEvents.ANVIL_USE,
                SoundSource.PLAYERS, 0.72F, 1.42F);
    }

    private static Optional<BlacksmithHeatJob.Target> findTarget(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && recipe(player.serverLevel(), stack).isPresent()) {
                return Optional.of(new BlacksmithHeatJob.ItemTarget(hand, stack));
            }
        }
        HitResult result = player.pick(BLOCK_REACH, 0.0F, false);
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.serverLevel().getBlockState(pos);
            if (state.is(FireMagicIds.BLACKSMITH_HEATABLE)) {
                return Optional.of(new BlacksmithHeatJob.BlockTarget(pos, state.getBlock()));
            }
        }
        return Optional.empty();
    }

    private static boolean complete(ServerPlayer player, BlacksmithHeatJob.Target target) {
        if (target instanceof BlacksmithHeatJob.ItemTarget item) return smelt(player, item);
        return heatBlock(player, (BlacksmithHeatJob.BlockTarget) target);
    }

    private static boolean smelt(ServerPlayer player, BlacksmithHeatJob.ItemTarget target) {
        ItemStack input = player.getItemInHand(target.hand());
        if (input.isEmpty() || !ItemStack.isSameItemSameTags(input, target.expected())) return false;
        Optional<SmeltingRecipe> recipe = recipe(player.serverLevel(), input);
        if (recipe.isEmpty()) return false;
        ItemStack output = recipe.orElseThrow().getResultItem(player.serverLevel().registryAccess()).copy();
        if (output.isEmpty()) return false;
        input.shrink(1);
        if (input.isEmpty()) {
            player.setItemInHand(target.hand(), output);
            return true;
        }
        player.getInventory().add(output);
        if (!output.isEmpty()) player.drop(output, false);
        return true;
    }

    private static boolean heatBlock(ServerPlayer player, BlacksmithHeatJob.BlockTarget target) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(target.pos());
        if (state.getBlock() != target.expected() || !state.is(FireMagicIds.BLACKSMITH_HEATABLE)) return false;
        BlacksmithHeatBlockEvent event = new BlacksmithHeatBlockEvent(level, player, target.pos(), state);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.handled()) return true;
        var property = state.getBlock().getStateDefinition().getProperty("heated");
        if (!(property instanceof BooleanProperty heated)) return false;
        return level.setBlock(target.pos(), state.setValue(heated, true), 3);
    }

    private static Optional<SmeltingRecipe> recipe(ServerLevel level, ItemStack input) {
        ItemStack single = input.copy();
        single.setCount(1);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                new SimpleContainer(single), level);
    }

    private static void particles(ServerPlayer player, BlacksmithHeatJob.Target target) {
        var center = target instanceof BlacksmithHeatJob.BlockTarget block
                ? net.minecraft.world.phys.Vec3.atCenterOf(block.pos())
                : player.getEyePosition().add(player.getLookAngle().scale(0.55D)).add(0.0D, -0.3D, 0.0D);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y, center.z,
                18, 0.2D, 0.2D, 0.2D, 0.025D);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z,
                3, 0.12D, 0.12D, 0.12D, 0.0D);
    }

    private static BlockPos targetPosition(ServerPlayer player, BlacksmithHeatJob.Target target) {
        return target instanceof BlacksmithHeatJob.BlockTarget block ? block.pos() : player.blockPosition();
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
