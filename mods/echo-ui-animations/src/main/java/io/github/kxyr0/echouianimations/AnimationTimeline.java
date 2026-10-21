package io.github.kxyr0.echouianimations;

import java.util.Objects;

/**
 * Deterministic sampler for animation presets.
 */
public final class AnimationTimeline {
    private AnimationTimeline() {
    }

    public static AnimationFrame sample(AnimationPreset preset, long elapsedMs, int totalCharacters) {
        AnimationPreset current = Objects.requireNonNull(preset, "preset");
        float linear = normalizedProgress(elapsedMs, current.delayMs(), current.durationMs());
        float eased = clamp(current.easing().apply(linear));
        int visible = visibleCharacters(current.revealsText(), eased, totalCharacters);
        return new AnimationFrame(
                eased,
                current.alpha().sample(eased),
                current.scale().sample(eased),
                current.widthFactor().sample(eased),
                current.heightFactor().sample(eased),
                visible,
                linear >= 1.0F);
    }

    private static float normalizedProgress(long elapsedMs, int delayMs, int durationMs) {
        if (elapsedMs <= delayMs) {
            return 0.0F;
        }
        return clamp((elapsedMs - delayMs) / (float) durationMs);
    }

    private static int visibleCharacters(boolean revealsText, float progress, int totalCharacters) {
        int safeTotal = Math.max(0, totalCharacters);
        if (!revealsText) {
            return safeTotal;
        }
        return Math.min(safeTotal, Math.round(safeTotal * progress));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
