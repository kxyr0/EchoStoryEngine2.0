package io.github.kxyr0.echoplayer.api.identity;

import java.util.Objects;

/**
 * Plain Echo display name after deterministic validation.
 */
public record PlayerDisplayName(String value) {
    public static final int MAX_CODE_POINTS = 48;

    public PlayerDisplayName {
        value = normalize(Objects.requireNonNull(value, "value"));
        validate(value);
    }

    public static PlayerDisplayName of(String value) {
        return new PlayerDisplayName(value);
    }

    private static String normalize(String value) {
        return value.strip();
    }

    private static void validate(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank.");
        }
        if (value.codePointCount(0, value.length()) > MAX_CODE_POINTS) {
            throw new IllegalArgumentException("Display name is longer than 48 Unicode code points.");
        }
        value.codePoints().forEach(codePoint -> {
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException("Display name must not contain control characters.");
            }
        });
    }
}
