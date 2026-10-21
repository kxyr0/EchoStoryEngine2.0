package io.github.kxyr0.echomagicweapons.forge.combat;

import io.github.kxyr0.echomagic.forge.api.event.MagicLinkCutEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

final class VoidWeaponLogic {
    private static final long MARK_TICKS = 200L;
    private final Map<UUID, Mark> marks = new HashMap<>();

    void damage(LivingDamageEvent event, ServerPlayer player, LivingEntity target) {
        long tick = player.serverLevel().getGameTime();
        Mark mark = marks.get(player.getUUID());
        boolean spatial = mark != null && mark.target().equals(target.getUUID()) && mark.expiresAt() >= tick;
        event.setAmount(spatial ? 4.0F : 0.5F);
        marks.put(player.getUUID(), new Mark(target.getUUID(), tick + MARK_TICKS));
        MobEffectInstance effect = target.getActiveEffects().stream().findFirst().orElse(null);
        if (effect != null) target.removeEffect(effect.getEffect());
        MinecraftForge.EVENT_BUS.post(new MagicLinkCutEvent(player, target));
        particles(player.serverLevel(), target);
    }

    void tick(ServerPlayer player) {
        Mark mark = marks.get(player.getUUID());
        if (mark == null) return;
        long tick = player.serverLevel().getGameTime();
        Entity entity = player.serverLevel().getEntity(mark.target());
        if (mark.expiresAt() < tick || !(entity instanceof LivingEntity target) || !target.isAlive()) {
            marks.remove(player.getUUID());
        } else if (tick % 8L == 0L) {
            particles(player.serverLevel(), target);
        }
    }

    void release(ServerPlayer player, int chargeTicks) {
        if (chargeTicks < 12) return;
        Mark mark = marks.get(player.getUUID());
        if (mark == null || mark.expiresAt() < player.serverLevel().getGameTime()) return;
        Entity entity = player.serverLevel().getEntity(mark.target());
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        Vec3 backwards = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (backwards.lengthSqr() < 1.0E-6D) backwards = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 destination = target.position().subtract(backwards.normalize().scale(1.35D));
        Vec3 origin = player.position();
        if (!WeaponEffects.safeTeleport(player, destination)) return;
        WeaponEffects.dust(player.serverLevel(), origin.add(0.0D, 1.0D, 0.0D), 0x0C692D, 0.9F, 24, 0.42D);
        WeaponEffects.dust(player.serverLevel(), destination.add(0.0D, 1.0D, 0.0D), 0x39FF75, 1.0F, 30, 0.42D);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 1.35F);
    }

    void clear(UUID player) { marks.remove(player); }
    void reset() { marks.clear(); }

    private void particles(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        for (int index = 0; index < 10; index++) {
            double angle = index * Math.PI * 0.2D;
            WeaponEffects.dust(level, center.add(Math.cos(angle) * 0.5D, 0.0D,
                    Math.sin(angle) * 0.5D), index % 2 == 0 ? 0x37FF70 : 0x06431D, 0.65F, 1, 0.01D);
        }
    }

    private record Mark(UUID target, long expiresAt) { }
}
