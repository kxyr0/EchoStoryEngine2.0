package io.github.kxyr0.echomagicweapons.forge.combat;

import io.github.kxyr0.echomagicweapons.forge.item.ArsenalWeaponKind;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

final class WaterWeaponLogic {
    private final Map<UUID, Float> previousYaw = new HashMap<>();
    private final Map<UUID, Boolean> returning = new HashMap<>();

    void tick(ServerPlayer player) { previousYaw.put(player.getUUID(), player.getYRot()); }

    void swing(ServerPlayer player) {
        returning.compute(player.getUUID(), (key, old) -> old == null || !old);
    }

    void hit(ServerPlayer player, LivingEntity target) {
        float oldYaw = previousYaw.getOrDefault(player.getUUID(), player.getYRot());
        float mouse = Mth.clamp(Mth.wrapDegrees(player.getYRot() - oldYaw) / 18.0F, -1.0F, 1.0F);
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        boolean isReturning = returning.getOrDefault(player.getUUID(), false);
        Vec3 motion = isReturning ? player.position().subtract(target.position()).normalize().scale(0.75D)
                : look.scale(0.36D).add(right.scale(mouse * 0.9D)).add(0.0D, 0.16D, 0.0D);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.25D).add(motion));
        target.hurtMarked = true;
        if (target.isOnFire()) target.clearFire();
        player.serverLevel().sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.55D), target.getZ(),
                18, 0.35D, 0.4D, 0.35D, 0.16D);
    }

    void pull(ServerPlayer player) {
        Entity target = WeaponEffects.rayTarget(player, 9.0D, entity -> entity instanceof ItemEntity
                || entity instanceof LivingEntity living && living.getMaxHealth() <= 24.0F);
        if (target == null) return;
        Vec3 destination = player.getEyePosition().add(0.0D, -0.7D, 0.0D);
        Vec3 pull = destination.subtract(target.position()).normalize().scale(target instanceof ItemEntity ? 1.25D : 0.72D);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.2D).add(pull));
        target.hurtMarked = true;
        WeaponEffects.line(player.serverLevel(), player.getEyePosition(), target.getBoundingBox().getCenter(),
                ArsenalWeaponKind.WATER.color(), 0.7F, 18);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    void clear(UUID player) { previousYaw.remove(player); returning.remove(player); }
    void reset() { previousYaw.clear(); returning.clear(); }
}
