package io.github.kxyr0.echonpc.api.appearance;

import java.util.Locale;
import java.util.Objects;

/**
 * RGB color for authored NPC name presentation.
 */
public record NpcNameColor(int rgb) {
    private static final String RGB_HEX_PATTERN = "[0-9A-Fa-f]{6}";
    private static final String OPAQUE_RGBA_HEX_PATTERN = "[0-9A-Fa-f]{6}[Ff]{2}";

    public NpcNameColor {
        if ((rgb & 0xFF000000) != 0) {
            throw new IllegalArgumentException("NpcNameColor must be a 24-bit RGB color.");
        }
    }

    public static NpcNameColor rgb(int rgb) {
        return new NpcNameColor(rgb);
    }

    public static NpcNameColor parse(String raw) {
        String value = Objects.requireNonNull(raw, "raw").trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.regionMatches(true, 0, "0x", 0, 2)) {
            value = value.substring(2);
        }
        if (value.matches(OPAQUE_RGBA_HEX_PATTERN)) {
            value = value.substring(0, 6);
        }
        if (!value.matches(RGB_HEX_PATTERN)) {
            throw new IllegalArgumentException(
                    "NpcNameColor must use RGB hex or opaque RGBA hex.");
        }
        return new NpcNameColor(Integer.parseInt(value, 16));
    }

    public String hex() {
        return "#" + String.format(Locale.ROOT, "%06X", rgb);
    }

    @Override
    public String toString() {
        return hex();
    }
}
