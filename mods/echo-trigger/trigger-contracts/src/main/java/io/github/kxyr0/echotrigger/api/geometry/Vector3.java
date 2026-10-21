package io.github.kxyr0.echotrigger.api.geometry;

public record Vector3(double X, double Y, double Z) {
    public Vector3 {
        requireFinite(X, "X");
        requireFinite(Y, "Y");
        requireFinite(Z, "Z");
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(X + other.X(), Y + other.Y(), Z + other.Z());
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(X - other.X(), Y - other.Y(), Z - other.Z());
    }

    public Vector3 divide(double divisor) {
        requireFinite(divisor, "divisor");
        if (divisor == 0.0D) {
            throw new IllegalArgumentException("Vector3 divisor must not be zero.");
        }
        return new Vector3(X / divisor, Y / divisor, Z / divisor);
    }

    public String stableText() {
        return number(X) + "," + number(Y) + "," + number(Z);
    }

    private static String number(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Vector3 " + name + " must be finite.");
        }
    }
}
