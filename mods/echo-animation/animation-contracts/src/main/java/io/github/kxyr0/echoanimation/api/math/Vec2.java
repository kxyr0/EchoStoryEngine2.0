package io.github.kxyr0.echoanimation.api.math;

/**
 * Immutable two-dimensional vector.
 */
public record Vec2(double x, double y) {
    public static final Vec2 ZERO = new Vec2(0.0D, 0.0D);

    public Vec2 {
        requireFinite(x, "x");
        requireFinite(y, "y");
    }

    public Vec2 plus(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 times(double scale) {
        return new Vec2(x * scale, y * scale);
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Vec2." + field + " должен быть finite.");
        }
    }
}

