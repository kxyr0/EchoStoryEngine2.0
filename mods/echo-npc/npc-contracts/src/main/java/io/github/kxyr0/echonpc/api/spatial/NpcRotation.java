package io.github.kxyr0.echonpc.api.spatial;

/**
 * Neutral yaw/pitch rotation. N1 uses direct presentation orientation only.
 */
public record NpcRotation(float yaw, float pitch) {
    public NpcRotation {
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
        if (pitch < -90.0F || pitch > 90.0F) {
            throw new IllegalArgumentException("pitch must be within [-90, 90].");
        }
    }

    public static NpcRotation zero() {
        return new NpcRotation(0.0F, 0.0F);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
