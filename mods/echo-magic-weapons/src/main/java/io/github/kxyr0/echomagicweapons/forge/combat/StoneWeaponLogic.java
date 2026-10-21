package io.github.kxyr0.echomagicweapons.forge.combat;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;

final class StoneWeaponLogic {
    void tick(ServerPlayer player) {
        if (!player.isUsingItem()) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.42D, motion.y, motion.z * 0.42D);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 2, false, false, false));
    }

    void knockback(LivingKnockBackEvent event, ServerPlayer player) {
        if (player.isUsingItem()) event.setStrength(event.getStrength() * 0.2F);
    }

    void hit(ServerPlayer player, LivingEntity target) {
        if (player.getXRot() < 50.0F && player.fallDistance < 0.6F) return;
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 from = target.position();
        Vec3 to = from.add(look.scale(4.0D));
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        for (int step = 0; step <= 12; step++) {
            Vec3 point = from.lerp(to, step / 12.0D);
            level.sendParticles(stone, point.x, point.y + 0.12D, point.z, 5, 0.22D, 0.08D, 0.22D, 0.05D);
        }
        for (LivingEntity victim : WeaponEffects.along(player, from, to, 1.1D)) {
            if (victim == target) continue;
            WeaponEffects.damage(player, victim, 2.5F);
            victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
            victim.hurtMarked = true;
        }
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.55F, 0.65F);
    }
}
