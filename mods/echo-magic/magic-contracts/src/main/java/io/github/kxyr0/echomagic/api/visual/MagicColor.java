package io.github.kxyr0.echomagic.api.visual;

public record MagicColor(float red, float green, float blue) {
    public MagicColor {
        if (!valid(red) || !valid(green) || !valid(blue)) throw new IllegalArgumentException("Color channels must be 0..1.");
    }

    public static MagicColor fromHex(String value) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid RGB color.");
        return new MagicColor(Integer.parseInt(value.substring(1, 3), 16) / 255.0F,
                Integer.parseInt(value.substring(3, 5), 16) / 255.0F,
                Integer.parseInt(value.substring(5, 7), 16) / 255.0F);
    }

    private static boolean valid(float value) { return Float.isFinite(value) && value >= 0.0F && value <= 1.0F; }
}
