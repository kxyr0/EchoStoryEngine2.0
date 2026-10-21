package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class GestureLinkScheduler {
    private static final double TARGET_RANGE = 18.0D;
    private static final double MINIMUM_AIM_COSINE = 0.72D;
    private static final double EXTRA_TRIGGER_DISTANCE = 3.0D;
    private static final long LINK_LIFETIME_TICKS = 400L;
    private final List<Link> links = new ArrayList<>();

    Optional<LivingEntity> findTarget(ForgeMagicServices services, ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        AABB search = caster.getBoundingBox().inflate(TARGET_RANGE);
        return caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                        target -> services.targetFilter().valid(caster, target, false)
                                && aimedAt(eye, look, target))
                .stream().min(Comparator.comparingDouble(target -> score(eye, look, target)));
    }

    void link(ForgeMagicServices services, MagicPlayerRef caster, SpellCastId cast,
              MagicGestureSymbol school, ServerLevel level, Vec3 anchor,
              LivingEntity target, long tick) {
        if (school == MagicGestureSymbol.VOID) {
            ServerPlayer player = services.player(caster).orElse(null);
            if (player != null) GesturePositionSwap.trySwap(level, player, target);
            services.cancelGestureCast(cast);
            return;
        }
        Vec3 targetPoint = center(target);
        links.add(new Link(caster, cast, school, level.dimension(), anchor, target.getUUID(), targetPoint,
                anchor.distanceTo(targetPoint), tick + LINK_LIFETIME_TICKS));
    }

    void tick(ForgeMagicServices services, long tick) {
        Iterator<Link> iterator = links.iterator();
        while (iterator.hasNext()) {
            Link link = iterator.next();
            ServerPlayer caster = services.player(link.caster).orElse(null);
            if (caster == null || !caster.isAlive()
                    || !caster.serverLevel().dimension().equals(link.dimension)) {
                services.cancelGestureCast(link.cast);
                iterator.remove();
                continue;
            }
            LivingEntity target = target(caster.serverLevel(), link.target);
            if (target != null) link.lastTarget = center(target);
            if (tick % 3L == 0L) particles(caster.serverLevel(), link);
            boolean removed = target == null || !target.isAlive() || target.isRemoved();
            boolean distant = link.anchor.distanceTo(link.lastTarget)
                    >= Math.min(TARGET_RANGE, link.initialDistance + EXTRA_TRIGGER_DISTANCE);
            if (removed || distant) {
                trigger(services, caster.serverLevel(), link, tick);
                iterator.remove();
            } else if (tick >= link.expiresTick) {
                services.cancelGestureCast(link.cast);
                iterator.remove();
            }
        }
    }

    void playerLeft(ForgeMagicServices services, MagicPlayerRef player) {
        links.removeIf(link -> {
            if (!link.caster.equals(player)) return false;
            services.cancelGestureCast(link.cast);
            return true;
        });
    }

    void reset() { links.clear(); }

    private static void trigger(ForgeMagicServices services, ServerLevel level, Link link, long tick) {
        Vec3 delta = link.lastTarget.subtract(link.anchor);
        if (delta.lengthSqr() <= 1.0E-8D) { services.cancelGestureCast(link.cast); return; }
        services.retargetGestureCast(link.cast, link.anchor, delta.normalize());
        services.activateGestureCast(link.cast, tick);
        DustParticleOptions color = color(link.school, 1.15F);
        level.sendParticles(color, link.lastTarget.x, link.lastTarget.y, link.lastTarget.z,
                18, 0.22D, 0.22D, 0.22D, 0.01D);
    }

    private static void particles(ServerLevel level, Link link) {
        Vec3 delta = link.lastTarget.subtract(link.anchor);
        int steps = Math.min(24, Math.max(6, (int) Math.ceil(delta.length() * 2.0D)));
        DustParticleOptions color = color(link.school, 0.72F);
        for (int index = 0; index <= steps; index++) {
            Vec3 point = link.anchor.add(delta.scale(index / (double) steps));
            level.sendParticles(color, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static LivingEntity target(ServerLevel level, UUID id) {
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean aimedAt(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 direction = center(target).subtract(eye);
        return direction.lengthSqr() <= TARGET_RANGE * TARGET_RANGE
                && direction.normalize().dot(look) >= MINIMUM_AIM_COSINE;
    }

    private static double score(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 direction = center(target).subtract(eye);
        double distance = direction.length();
        return (1.0D - direction.normalize().dot(look)) * 12.0D + distance * 0.08D;
    }

    private static Vec3 center(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static DustParticleOptions color(MagicGestureSymbol school, float size) {
        Vector3f rgb = switch (school) {
            case FIRE -> new Vector3f(1.0F, 0.18F, 0.02F);
            case WATER -> new Vector3f(0.05F, 0.45F, 1.0F);
            case EARTH -> new Vector3f(0.52F, 0.32F, 0.08F);
            case AIR -> new Vector3f(0.72F, 0.95F, 1.0F);
            case BLOOD -> new Vector3f(1.0F, 0.02F, 0.02F);
            case VOID -> new Vector3f(0.08F, 1.0F, 0.18F);
        };
        return new DustParticleOptions(rgb, size);
    }

    private static final class Link {
        private final MagicPlayerRef caster;
        private final SpellCastId cast;
        private final MagicGestureSymbol school;
        private final ResourceKey<Level> dimension;
        private final Vec3 anchor;
        private final UUID target;
        private final double initialDistance;
        private final long expiresTick;
        private Vec3 lastTarget;

        private Link(MagicPlayerRef caster, SpellCastId cast, MagicGestureSymbol school,
                     ResourceKey<Level> dimension, Vec3 anchor, UUID target, Vec3 lastTarget,
                     double initialDistance, long expiresTick) {
            this.caster = caster; this.cast = cast; this.school = school; this.dimension = dimension;
            this.anchor = anchor; this.target = target; this.lastTarget = lastTarget;
            this.initialDistance = initialDistance; this.expiresTick = expiresTick;
        }
    }
}
