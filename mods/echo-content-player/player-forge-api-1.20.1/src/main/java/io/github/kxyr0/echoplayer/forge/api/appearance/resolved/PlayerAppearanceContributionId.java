package io.github.kxyr0.echoplayer.forge.api.appearance.resolved;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable namespace:path identity for a public appearance contributor.
 */
public record PlayerAppearanceContributionId(String value) {
    private static final Pattern PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public PlayerAppearanceContributionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Contribution id must be namespace:path: " + value);
        }
    }
}
