package io.github.kxyr0.echomagicweapons.forge.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

final class AirWeaponLogic {
    private final Set<UUID> parried = new HashSet<>();

    void damage(LivingDamageEvent event, ServerPlayer player) {
        float damage = 1.0F;
        if (player.isSprinting()) damage += 3.0F;
        if (!player.onGround()) damage += 2.5F;
        event.setAmount(damage);
    }

    void tick(ServerPlayer player) {
        if (!player.isUsingItem()) return;
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(2.4D);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && !parried.contains(projectile.getUUID()))) {
            Vec3 velocity = projectile.getDeltaMovement();
            projectile.setDeltaMovement(velocity.scale(-1.15D).add(player.getLookAngle().scale(0.35D)));
            projectile.setOwner(player);
            projectile.hurtMarked = true;
            parried.add(projectile.getUUID());
            level.sendParticles(ParticleTypes.CLOUD, projectile.getX(), projectile.getY(), projectile.getZ(),
                    12, 0.22D, 0.22D, 0.22D, 0.08D);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.7F, 1.65F);
        }
        if (parried.size() > 256) parried.clear();
    }

    void release(ServerPlayer player, int chargeTicks) {
        if (chargeTicks < 12) return;
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 requested = from.add(direction.scale(4.5D));
        Vec3 destination = requested;
        while (!WeaponEffects.safeTeleport(player, destination) && destination.distanceTo(from) > 1.0D) {
            destination = destination.subtract(direction.scale(0.5D));
        }
        if (player.position().distanceToSqr(from) < 0.25D) return;
        for (LivingEntity target : WeaponEffects.along(player, from, destination, 1.0D)) {
            WeaponEffects.damage(player, target, 3.0F);
        }
        WeaponEffects.line(level, from.add(0.0D, 1.0D, 0.0D), destination.add(0.0D, 1.0D, 0.0D),
                0xE8FFFF, 0.65F, 24);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.65F);
    }

    void reset() { parried.clear(); }
}
