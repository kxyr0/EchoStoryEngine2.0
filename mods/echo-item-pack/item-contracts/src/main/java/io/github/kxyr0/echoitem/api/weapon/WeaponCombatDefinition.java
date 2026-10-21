package io.github.kxyr0.echoitem.api.weapon;

/**
 * Runtime combat values for a melee weapon.
 */
public record WeaponCombatDefinition(double attackDamage, double attacksPerSecond, double knockback) {
    public WeaponCombatDefinition {
        if (attackDamage <= 0.0D || attackDamage > 1024.0D) {
            throw new IllegalArgumentException("Attack damage must be positive and bounded.");
        }
        if (attacksPerSecond <= 0.0D || attacksPerSecond > 20.0D) {
            throw new IllegalArgumentException("Attacks per second must be positive and bounded.");
        }
        if (knockback < 0.0D || knockback > 32.0D) {
            throw new IllegalArgumentException("Knockback must be bounded.");
        }
    }

    public double minecraftAttackSpeedModifier() {
        return attacksPerSecond - 4.0D;
    }
}
