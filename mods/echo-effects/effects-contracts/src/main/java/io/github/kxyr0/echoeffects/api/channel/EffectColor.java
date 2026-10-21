package io.github.kxyr0.echoeffects.api.channel;

public record EffectColor(int red, int green, int blue) {
    public EffectColor {
        requireByte(red, "red");
        requireByte(green, "green");
        requireByte(blue, "blue");
    }

    public static EffectColor hex(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Color must be #RRGGBB.");
        }
        return new EffectColor(
                Integer.parseInt(value.substring(1, 3), 16),
                Integer.parseInt(value.substring(3, 5), 16),
                Integer.parseInt(value.substring(5, 7), 16));
    }

    private static void requireByte(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be in [0, 255].");
        }
    }
}
