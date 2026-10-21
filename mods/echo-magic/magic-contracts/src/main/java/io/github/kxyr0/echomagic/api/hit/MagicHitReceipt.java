package io.github.kxyr0.echomagic.api.hit;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import java.util.Objects;
import java.util.UUID;

public record MagicHitReceipt(SpellProjectileId projectileId, SpellCastId castId, UUID target,
        MagicDamageTypeId damageType, double amount, long serverTick, boolean applied) {
    public MagicHitReceipt {
        projectileId = Objects.requireNonNull(projectileId, "projectileId");
        castId = Objects.requireNonNull(castId, "castId");
        target = Objects.requireNonNull(target, "target");
        damageType = Objects.requireNonNull(damageType, "damageType");
        if (!Double.isFinite(amount) || amount <= 0.0D) throw new IllegalArgumentException("Invalid damage amount.");
    }
}
