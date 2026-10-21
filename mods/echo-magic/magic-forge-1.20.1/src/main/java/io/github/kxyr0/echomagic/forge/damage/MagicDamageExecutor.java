package io.github.kxyr0.echomagic.forge.damage;

import io.github.kxyr0.echomagic.api.hit.MagicHitReceipt;
import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.hit.SpellKnockbackDirection;
import io.github.kxyr0.echomagic.core.hit.MagicHitLedger;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectilePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class MagicDamageExecutor {
    private final MagicHitLedger ledger;
    public MagicDamageExecutor(MagicHitLedger ledger) { this.ledger = ledger; }

    public boolean apply(MagicProjectileEntity direct, MagicProjectilePayload payload, LivingEntity target,
                         long serverTick) {
        SpellProjectileId projectileId = new SpellProjectileId(payload.projectileId());
        if (ledger.find(projectileId).isPresent()) return false;
        ServerLevel level = (ServerLevel) direct.level();
        Entity caster = level.getEntity(payload.caster());
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                new ResourceLocation(payload.damageType()));
        boolean applied;
        try {
            var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
            applied = target.hurt(new DamageSource(type, direct, caster), (float) payload.damageAmount());
        }
        catch (RuntimeException exception) { applied = false; }
        applyEffects(direct, payload, target);
        ledger.record(new MagicHitReceipt(projectileId, new SpellCastId(payload.castId()), target.getUUID(),
                new MagicDamageTypeId(payload.damageType()), payload.damageAmount(), serverTick, applied));
        return applied;
    }

    private static void applyEffects(MagicProjectileEntity direct, MagicProjectilePayload payload,
                                     LivingEntity target) {
        if (payload.extinguish()) target.clearFire();
        if (payload.igniteTicks() > 0) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), payload.igniteTicks()));
        }
        if (payload.knockback() <= 0.0D) return;
        var velocity = direct.getDeltaMovement();
        var horizontal = new net.minecraft.world.phys.Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontal.lengthSqr() <= 1.0E-8D) return;
        horizontal = horizontal.normalize();
        if (SpellKnockbackDirection.LATERAL.name().equals(payload.knockbackDirection())) {
            horizontal = new net.minecraft.world.phys.Vec3(-horizontal.z, 0.0D, horizontal.x);
        }
        target.knockback(payload.knockback(), -horizontal.x, -horizontal.z);
    }
}
