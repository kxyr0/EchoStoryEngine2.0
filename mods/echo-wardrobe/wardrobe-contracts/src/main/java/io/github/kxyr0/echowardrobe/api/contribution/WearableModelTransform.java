package io.github.kxyr0.echowardrobe.api.contribution;

/**
 * Standard item-model transform applied relative to the mapped body slot.
 */
public record WearableModelTransform(
        double x,
        double y,
        double z,
        double pitch,
        double yaw,
        double roll,
        double scaleX,
        double scaleY,
        double scaleZ
) {
    public WearableModelTransform {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(pitch, "pitch");
        requireFinite(yaw, "yaw");
        requireFinite(roll, "roll");
        requirePositive(scaleX, "scaleX");
        requirePositive(scaleY, "scaleY");
        requirePositive(scaleZ, "scaleZ");
    }

    public static WearableModelTransform identity() {
        return new WearableModelTransform(0D, 0D, 0D, 0D, 0D, 0D, 1D, 1D, 1D);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static void requirePositive(double value, String name) {
        requireFinite(value, name);
        if (value <= 0D) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
