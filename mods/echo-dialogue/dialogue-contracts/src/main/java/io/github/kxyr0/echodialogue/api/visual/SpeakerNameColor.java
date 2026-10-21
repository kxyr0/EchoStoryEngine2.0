package io.github.kxyr0.echodialogue.api.visual;

import java.util.Locale;
import java.util.Objects;

/**
 * Optional RGB color for speaker name text.
 */
public record SpeakerNameColor(int rgb) {
    private static final String RGB_HEX_PATTERN = "[0-9A-Fa-f]{6}";
    private static final String OPAQUE_RGBA_HEX_PATTERN = "[0-9A-Fa-f]{6}[Ff]{2}";

    public SpeakerNameColor {
        if ((rgb & 0xFF000000) != 0) {
            throw new IllegalArgumentException("SpeakerNameColor must be a 24-bit RGB color.");
        }
    }

    public static SpeakerNameColor rgb(int rgb) {
        return new SpeakerNameColor(rgb);
    }

    public static SpeakerNameColor parse(String raw) {
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
                    "SpeakerNameColor must use RGB hex or opaque RGBA hex.");
        }
        return new SpeakerNameColor(Integer.parseInt(value, 16));
    }

    public String hex() {
        return "#" + String.format(Locale.ROOT, "%06X", rgb);
    }

    @Override
    public String toString() {
        return hex();
    }
}
