package io.github.kxyr0.echomagicweapons.forge.combat;

import io.github.kxyr0.echomagicweapons.forge.EchoMagicWeaponsMod;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class WeaponEffects {
    private WeaponEffects() { }

    static void dust(ServerLevel level, Vec3 point, int color, float size, int count, double spread) {
        Vector3f rgb = new Vector3f(((color >> 16) & 255) / 255.0F,
                ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
        level.sendParticles(new DustParticleOptions(rgb, size), point.x, point.y, point.z,
                count, spread, spread, spread, 0.0D);
    }

    static void line(ServerLevel level, Vec3 from, Vec3 to, int color, float size, int points) {
        for (int index = 0; index <= points; index++) {
            dust(level, from.lerp(to, index / (double) points), color, size, 1, 0.01D);
        }
    }

    static Entity rayTarget(ServerPlayer player, double range, Predicate<Entity> accepted) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB bounds = new AABB(start, end).inflate(1.25D);
        return player.serverLevel().getEntities(player, bounds, accepted).stream()
                .filter(entity -> distanceToSegment(entity.getBoundingBox().getCenter(), start, end)
                        <= Math.max(0.7D, entity.getBbWidth()))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);
    }

    static List<LivingEntity> along(ServerPlayer player, Vec3 from, Vec3 to, double radius) {
        AABB bounds = new AABB(from, to).inflate(radius);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != player && entity.isAlive()
                        && distanceToSegment(entity.getBoundingBox().getCenter(), from, to) <= radius);
    }

    static boolean safeTeleport(ServerPlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        if (!player.serverLevel().noCollision(player, player.getBoundingBox().move(delta))) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        return true;
    }

    static boolean damage(ServerPlayer player, LivingEntity target, float amount) {
        return EchoMagicWeaponsMod.runtime().specialDamage(player, target, amount);
    }

    private static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 line = to.subtract(from);
        double length = line.lengthSqr();
        if (length < 1.0E-7D) return point.distanceTo(from);
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(from).dot(line) / length));
        return point.distanceTo(from.add(line.scale(t)));
    }
}
