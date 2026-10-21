package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class GestureSpellScheduler {
    private static final long SEAL_ARM_TICKS = 10L;
    private static final long SEAL_LIFETIME_TICKS = 600L;
    private static final double SEAL_TRIGGER_RADIUS = 1.6D;
    private final List<Marker> markers = new ArrayList<>();

    void delayed(MagicPlayerRef caster, SpellCastId cast, MagicGestureSymbol school,
                 ServerLevel level, Vec3 origin, long dueTick, boolean friendlyFire) {
        markers.add(new Marker(caster, cast, school, level.dimension(), origin, dueTick, dueTick,
                friendlyFire, false));
    }

    void seal(MagicPlayerRef caster, SpellCastId cast, MagicGestureSymbol school,
              ServerLevel level, Vec3 origin, long tick, boolean friendlyFire) {
        markers.add(new Marker(caster, cast, school, level.dimension(), origin, tick + SEAL_ARM_TICKS,
                tick + SEAL_LIFETIME_TICKS, friendlyFire, true));
    }

    void tick(ForgeMagicServices services, long tick) {
        Iterator<Marker> iterator = markers.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            ServerPlayer caster = services.player(marker.caster()).orElse(null);
            if (caster == null || !caster.isAlive()
                    || !caster.serverLevel().dimension().equals(marker.dimension())) {
                services.cancelGestureCast(marker.cast());
                iterator.remove();
                continue;
            }
            if (tick % 4L == 0L) particles(caster.serverLevel(), marker);
            if (!marker.triggered() && tick >= marker.armedTick()) {
                iterator.remove();
            } else if (marker.triggered() && tick >= marker.expiresTick()) {
                services.cancelGestureCast(marker.cast());
                iterator.remove();
            } else if (marker.triggered() && tick >= marker.armedTick()
                    && entered(services, caster, marker)) {
                services.activateGestureCast(marker.cast(), tick);
                iterator.remove();
            }
        }
    }

    void playerLeft(ForgeMagicServices services, MagicPlayerRef player) {
        markers.removeIf(marker -> {
            if (!marker.caster().equals(player)) return false;
            services.cancelGestureCast(marker.cast());
            return true;
        });
    }

    void reset() { markers.clear(); }

    private static boolean entered(ForgeMagicServices services, ServerPlayer caster, Marker marker) {
        AABB bounds = new AABB(marker.origin(), marker.origin()).inflate(SEAL_TRIGGER_RADIUS, 1.2D,
                SEAL_TRIGGER_RADIUS);
        List<LivingEntity> targets = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds,
                target -> services.targetFilter().valid(caster, target, marker.friendlyFire()));
        return !targets.isEmpty();
    }

    private static void particles(ServerLevel level, Marker marker) {
        DustParticleOptions color = color(marker.school());
        for (int index = 0; index < 16; index++) {
            double angle = Math.PI * 2.0D * index / 16.0D;
            double radius = marker.triggered() ? 1.25D : 0.32D;
            Vec3 point = marker.origin().add(Math.cos(angle) * radius, 0.08D,
                    Math.sin(angle) * radius);
            level.sendParticles(color, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static DustParticleOptions color(MagicGestureSymbol school) {
        Vector3f rgb = switch (school) {
            case FIRE -> new Vector3f(1.0F, 0.18F, 0.02F);
            case WATER -> new Vector3f(0.05F, 0.45F, 1.0F);
            case EARTH -> new Vector3f(0.52F, 0.32F, 0.08F);
            case AIR -> new Vector3f(0.72F, 0.95F, 1.0F);
            case BLOOD -> new Vector3f(1.0F, 0.02F, 0.02F);
            case VOID -> new Vector3f(0.08F, 1.0F, 0.18F);
        };
        return new DustParticleOptions(rgb, 0.85F);
    }

    private record Marker(MagicPlayerRef caster, SpellCastId cast, MagicGestureSymbol school,
                          ResourceKey<Level> dimension, Vec3 origin, long armedTick, long expiresTick,
                          boolean friendlyFire, boolean triggered) { }
}
