package io.github.kxyr0.echoplayervitals.api.hunger;

/**
 * Compatibility projection for vanilla hunger after mechanics are neutralized.
 */
public record PlayerHungerSnapshot(
        int foodLevel,
        float saturationLevel,
        float exhaustionLevel,
        boolean foodBasedRegeneration,
        boolean sprintRestrictedByFood,
        boolean starvationDamage
) {
    public PlayerHungerSnapshot {
        if (foodLevel < 0 || foodLevel > 20) {
            throw new IllegalArgumentException("Food level must stay in the vanilla 0..20 range.");
        }
        if (saturationLevel < 0.0F || exhaustionLevel < 0.0F) {
            throw new IllegalArgumentException("Saturation and exhaustion cannot be negative.");
        }
    }

    public static PlayerHungerSnapshot neutral() {
        return new PlayerHungerSnapshot(20, 0.0F, 0.0F, false, false, false);
    }
}
