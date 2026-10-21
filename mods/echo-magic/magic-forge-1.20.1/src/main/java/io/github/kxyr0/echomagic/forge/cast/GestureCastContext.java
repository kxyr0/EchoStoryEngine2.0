package io.github.kxyr0.echomagic.forge.cast;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;

public record GestureCastContext(Optional<Vec3> origin, Optional<Vec3> direction,
                                 long initialDelayTicks, boolean waitsForTrigger, boolean split) {
    public GestureCastContext {
        origin = origin == null ? Optional.empty() : origin;
        direction = direction == null ? Optional.empty() : direction.map(Vec3::normalize);
        if (initialDelayTicks < 0L) throw new IllegalArgumentException("Gesture delay must not be negative.");
        origin.ifPresent(value -> requireFinite(value, "origin"));
        direction.ifPresent(value -> {
            requireFinite(value, "direction");
            if (value.lengthSqr() <= 1.0E-8D) throw new IllegalArgumentException("Direction must not be zero.");
        });
    }

    public static GestureCastContext immediate() {
        return new GestureCastContext(Optional.empty(), Optional.empty(), 0L, false, false);
    }

    public static GestureCastContext aimed(Vec3 direction) {
        return new GestureCastContext(Optional.empty(), Optional.of(direction), 0L, false, false);
    }

    public static GestureCastContext delayed(Vec3 origin, Vec3 direction, long delayTicks) {
        return new GestureCastContext(Optional.of(origin), Optional.of(direction), delayTicks, false, false);
    }

    public static GestureCastContext seal(Vec3 origin, Vec3 direction) {
        return new GestureCastContext(Optional.of(origin), Optional.of(direction), 0L, true, false);
    }

    public static GestureCastContext splitCast() {
        return new GestureCastContext(Optional.empty(), Optional.empty(), 0L, false, true);
    }

    public GestureCastContext withAim(Vec3 newOrigin, Vec3 newDirection) {
        return new GestureCastContext(Optional.of(newOrigin), Optional.of(newDirection),
                initialDelayTicks, waitsForTrigger, split);
    }

    public GestureCastContext withDirection(Vec3 newDirection) {
        return new GestureCastContext(origin, Optional.of(newDirection), initialDelayTicks, waitsForTrigger, split);
    }

    private static void requireFinite(Vec3 value, String field) {
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) {
            throw new IllegalArgumentException("Gesture " + field + " must be finite.");
        }
    }
}
