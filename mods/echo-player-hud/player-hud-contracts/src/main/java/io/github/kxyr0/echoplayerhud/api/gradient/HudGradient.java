package io.github.kxyr0.echoplayerhud.api.gradient;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable stepped gradient definition.
 */
public record HudGradient(List<HudColorStep> steps) {
    public HudGradient {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps")).stream()
                .sorted(Comparator.comparingDouble(HudColorStep::at))
                .toList();
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("HUD gradient requires at least one color step.");
        }
    }

    public static HudGradient solid(int argb) {
        return new HudGradient(List.of(new HudColorStep(0.0D, argb)));
    }
}
