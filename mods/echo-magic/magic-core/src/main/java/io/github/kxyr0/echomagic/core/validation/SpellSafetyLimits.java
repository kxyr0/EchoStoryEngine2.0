package io.github.kxyr0.echomagic.core.validation;

public record SpellSafetyLimits(int maximumKnownSpells, int maximumActiveCasts, int maximumActiveProjectiles,
                                double maximumRange, double maximumSpeed, int maximumBurst, double maximumDamage) {
    public SpellSafetyLimits {
        if (maximumKnownSpells < 1 || maximumActiveCasts < 1 || maximumActiveProjectiles < 1
                || maximumRange <= 0.0D || maximumSpeed <= 0.0D || maximumBurst < 1 || maximumDamage <= 0.0D) {
            throw new IllegalArgumentException("All magic safety limits must be positive.");
        }
    }

    public static SpellSafetyLimits defaults() {
        return new SpellSafetyLimits(128, 8, 64, 128.0D, 8.0D, 32, 100.0D);
    }
}

