package io.github.kxyr0.echomagic.api.definition;

public record SpellProjectileDefinition(double speed, double range, double radius, double gravity, int pierce) {
    public SpellProjectileDefinition {
        if (!Double.isFinite(speed) || speed <= 0.0D) throw new IllegalArgumentException("Speed must be positive.");
        if (!Double.isFinite(range) || range <= 0.0D) throw new IllegalArgumentException("Range must be positive.");
        if (!Double.isFinite(radius) || radius < 0.0D) throw new IllegalArgumentException("Radius must not be negative.");
        if (!Double.isFinite(gravity) || gravity < 0.0D) throw new IllegalArgumentException("Gravity must not be negative.");
        if (pierce < 0) throw new IllegalArgumentException("Pierce must not be negative.");
    }
}

