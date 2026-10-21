package io.github.kxyr0.echomagicweapons.forge.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class FireWeaponLogic {
    private static final int MAX_HEAT = 5;
    private final Map<UUID, Integer> heat = new HashMap<>();
    private final Map<UUID, Long> lastSwing = new HashMap<>();

    void swing(ServerPlayer player) {
        long tick = player.serverLevel().getGameTime();
        Long previous = lastSwing.put(player.getUUID(), tick);
        if (previous != null && previous == tick) return;
        int current = heat.getOrDefault(player.getUUID(), 0);
        if (current >= MAX_HEAT) {
            wave(player);
            heat.put(player.getUUID(), 0);
        } else {
            heat.put(player.getUUID(), current + 1);
        }
    }

    void hit(ServerPlayer player, LivingEntity target) {
        swing(player);
        target.setSecondsOnFire(5);
        player.serverLevel().sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.6D), target.getZ(),
                10, 0.25D, 0.35D, 0.25D, 0.03D);
    }

    void plant(ServerPlayer player) {
        if (!player.isShiftKeyDown()) return;
        ServerLevel level = player.serverLevel();
        Vec3 forward = horizontal(player.getLookAngle());
        for (int step = 1; step <= 8; step++) {
            BlockPos column = BlockPos.containing(player.position().add(forward.scale(step)));
            placeFire(level, column);
            Vec3 point = Vec3.atCenterOf(column).add(0.0D, 0.5D, 0.0D);
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 5, 0.18D, 0.1D, 0.18D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    void clear(UUID player) { heat.remove(player); lastSwing.remove(player); }
    void reset() { heat.clear(); lastSwing.clear(); }

    private void wave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 look = horizontal(player.getLookAngle());
        for (int step = 1; step <= 11; step++) {
            Vec3 center = start.add(look.scale(step * 0.48D));
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                    3 + step / 2, step * 0.035D, 0.2D, step * 0.035D, 0.035D);
        }
        for (LivingEntity target : WeaponEffects.along(player, start, start.add(look.scale(5.5D)), 1.5D)) {
            WeaponEffects.damage(player, target, 4.0F);
            target.setSecondsOnFire(6);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private void placeFire(ServerLevel level, BlockPos column) {
        for (int offset = 2; offset >= -2; offset--) {
            BlockPos position = column.offset(0, offset, 0);
            BlockPos below = position.below();
            if (level.getBlockState(position).canBeReplaced()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(position, Blocks.FIRE.defaultBlockState(), 3);
                return;
            }
        }
    }

    private Vec3 horizontal(Vec3 value) {
        Vec3 result = new Vec3(value.x, 0.0D, value.z);
        return result.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 0.0D, 1.0D) : result.normalize();
    }
}
