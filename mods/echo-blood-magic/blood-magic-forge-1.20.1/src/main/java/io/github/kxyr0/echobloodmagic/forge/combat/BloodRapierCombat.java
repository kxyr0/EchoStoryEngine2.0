package io.github.kxyr0.echobloodmagic.forge.combat;

import io.github.kxyr0.echobloodmagic.forge.item.BloodRapierLease;
import io.github.kxyr0.echomagic.forge.api.event.MagicLinkCutEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class BloodRapierCombat {
    private static final long LINK_TICKS = 160L;
    private final Map<UUID, BloodLink> links = new HashMap<>();

    void link(LivingEntity target, ServerPlayer caster, long tick) {
        links.put(target.getUUID(), new BloodLink(target.level().dimension(), caster.getUUID(), tick + LINK_TICKS));
        BloodRapierParticles.link(caster.serverLevel(), caster, target);
    }

    void tick(MinecraftServer server, long tick) {
        Iterator<Map.Entry<UUID, BloodLink>> iterator = links.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BloodLink> entry = iterator.next();
            BloodLink link = entry.getValue();
            ServerLevel level = server.getLevel(link.dimension());
            Entity targetEntity = level == null ? null : level.getEntity(entry.getKey());
            ServerPlayer caster = server.getPlayerList().getPlayer(link.caster());
            if (tick > link.expiresAt() || !(targetEntity instanceof LivingEntity target)
                    || !target.isAlive() || caster == null || caster.level() != level) {
                iterator.remove();
                continue;
            }
            Vec3 delta = caster.position().subtract(target.position());
            if (delta.lengthSqr() > 6.25D) {
                target.setDeltaMovement(target.getDeltaMovement().add(delta.normalize().scale(0.045D)));
                target.hurtMarked = true;
            }
            if (tick % 4L == 0L) BloodRapierParticles.link(level, caster, target);
        }
    }

    boolean shatter(ServerPlayer player) {
        if (!player.isShiftKeyDown() || !BloodRapierLease.active(player)) return false;
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition().add(0.0D, -0.15D, 0.0D);
        Set<UUID> damaged = new HashSet<>();
        BloodRapierLease.restore(player);
        for (int needle = -3; needle <= 3; needle++) {
            Vec3 direction = player.getLookAngle().yRot((float) Math.toRadians(needle * 7.5D)).normalize();
            Vec3 end = start.add(direction.scale(9.0D));
            LivingEntity target = nearest(level, player, start, end);
            Vec3 actualEnd = target == null ? end : target.getBoundingBox().getCenter();
            BloodRapierParticles.needle(level, start, actualEnd);
            if (target != null && damaged.add(target.getUUID())) {
                target.hurt(player.damageSources().playerAttack(player), 3.0F);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    void cut(MagicLinkCutEvent event) {
        if (links.remove(event.target().getUUID()) != null) event.markHandled();
    }

    void clear(UUID caster) { links.entrySet().removeIf(entry -> entry.getValue().caster().equals(caster)); }
    void reset() { links.clear(); }

    private LivingEntity nearest(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        AABB area = new AABB(from, to).inflate(0.7D);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && distance(entity.position(), from, to) < 0.7D)
                .stream().min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);
    }

    private double distance(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 line = to.subtract(from);
        double factor = Math.max(0.0D, Math.min(1.0D, point.subtract(from).dot(line) / line.lengthSqr()));
        return point.distanceTo(from.add(line.scale(factor)));
    }
}
