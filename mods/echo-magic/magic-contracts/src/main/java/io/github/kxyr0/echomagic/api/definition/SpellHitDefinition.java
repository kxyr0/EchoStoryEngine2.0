package io.github.kxyr0.echomagic.api.definition;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.hit.SpellKnockbackDirection;
import java.util.Objects;

public record SpellHitDefinition(MagicDamageTypeId damageType, double amount, int igniteTicks,
        boolean extinguish, double knockback, SpellKnockbackDirection knockbackDirection,
        boolean deflectProjectiles) {
    public SpellHitDefinition(MagicDamageTypeId damageType, double amount) {
        this(damageType, amount, 0, false, 0.0D, SpellKnockbackDirection.FORWARD, false);
    }

    public SpellHitDefinition {
        damageType = Objects.requireNonNull(damageType, "damageType");
        knockbackDirection = Objects.requireNonNull(knockbackDirection, "knockbackDirection");
        if (!Double.isFinite(amount) || amount <= 0.0D) throw new IllegalArgumentException("Damage must be positive.");
        if (igniteTicks < 0) throw new IllegalArgumentException("Ignite duration must not be negative.");
        if (!Double.isFinite(knockback) || knockback < 0.0D || knockback > 4.0D) {
            throw new IllegalArgumentException("Knockback must be between 0 and 4.");
        }
    }
}
