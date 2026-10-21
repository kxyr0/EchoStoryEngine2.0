package io.github.kxyr0.echonpc.api.spatial;

/**
 * Bounded neutral world position for stationary N1 actors.
 */
public record NpcPosition(double x, double y, double z) {
    private static final double MAX_HORIZONTAL = 30_000_000.0D;
    private static final double MIN_Y = -2048.0D;
    private static final double MAX_Y = 4096.0D;

    public NpcPosition {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        if (Math.abs(x) > MAX_HORIZONTAL || Math.abs(z) > MAX_HORIZONTAL || y < MIN_Y || y > MAX_Y) {
            throw new IllegalArgumentException("NpcPosition is outside N1 bounds.");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
