package io.github.kxyr0.echoplayerhud.core.layout;

import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import java.util.List;
import java.util.Objects;

/**
 * Immutable published HUD layout.
 */
public record HudLayoutSnapshot(List<HudBarDefinition> bars, long revision, int referenceWidth,
                                int referenceHeight, boolean scaleWithResolution) {
    private static final int DEFAULT_REFERENCE_WIDTH = 1920;
    private static final int DEFAULT_REFERENCE_HEIGHT = 1080;

    public HudLayoutSnapshot(List<HudBarDefinition> bars, long revision) {
        this(bars, revision, DEFAULT_REFERENCE_WIDTH, DEFAULT_REFERENCE_HEIGHT, true);
    }

    public HudLayoutSnapshot(List<HudBarDefinition> bars, long revision, int referenceWidth,
                             int referenceHeight) {
        this(bars, revision, referenceWidth, referenceHeight, true);
    }

    public HudLayoutSnapshot {
        bars = List.copyOf(Objects.requireNonNull(bars, "bars"));
        if (revision < 0L) {
            throw new IllegalArgumentException("HUD layout revision cannot be negative.");
        }
        if (referenceWidth <= 0 || referenceHeight <= 0) {
            throw new IllegalArgumentException("HUD reference canvas must be positive.");
        }
    }
}
