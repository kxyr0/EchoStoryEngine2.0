package io.github.kxyr0.echomagic.forge.cast;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellDirection;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

record GestureCastProfile(GestureCastContext context, SpellBurstDefinition originalBurst) {
    GestureCastProfile withAim(Vec3 origin, Vec3 direction) {
        return new GestureCastProfile(context.withAim(origin, direction), originalBurst);
    }

    SpellDefinition runtimeDefinition(SpellDefinition original, int maximumBurst) {
        if (!context.split()) return original;
        int count = Math.min(maximumBurst, originalBurst.count() * 3);
        SpellBurstDefinition splitBurst = new SpellBurstDefinition(count, 0L, originalBurst.spreadDegrees());
        return new SpellDefinition(original.id(), original.revision(), original.displayName(), original.school(),
                original.technique(), original.cooldown(), original.cast(), splitBurst, original.projectile(),
                original.collision(), original.targets(), original.friendlyFire(), original.trail(), original.core(),
                original.hit(), original.casterDeathPolicy());
    }

    long firstEmissionTick(long acceptedTick) {
        if (context.waitsForTrigger()) return Long.MAX_VALUE;
        return acceptedTick > Long.MAX_VALUE - context.initialDelayTicks()
                ? Long.MAX_VALUE : acceptedTick + context.initialDelayTicks();
    }

    Vec3 origin(ServerPlayer player, SpellDefinition definition, Vec3 direction) {
        Vec3 base = context.origin().orElseGet(() -> switch (definition.cast().origin()) {
            case PLAYER_EYES -> player.getEyePosition();
            case PLAYER_CENTER -> player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        });
        return base.add(direction.scale(definition.cast().forwardOffset()));
    }

    Vec3 direction(ServerPlayer player, SpellDefinition definition, int emissionIndex) {
        Vec3 base = context.direction().orElseGet(() -> baseDirection(player, definition));
        if (!context.split() || originalBurst.spreadDegrees() >= 300.0D) {
            return spread(base, definition.burst().count(), emissionIndex, definition.burst().spreadDegrees());
        }
        int originalIndex = Math.min(originalBurst.count() - 1, emissionIndex / 3);
        Vec3 originalDirection = spread(base, originalBurst.count(), originalIndex,
                originalBurst.spreadDegrees());
        return rotate(originalDirection, (emissionIndex % 3 - 1) * 22.0D);
    }

    private static Vec3 baseDirection(ServerPlayer player, SpellDefinition definition) {
        Vec3 look = player.getLookAngle().normalize();
        if (definition.cast().direction() == SpellDirection.PLAYER_LOOK) return look;
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-8D) return horizontal.normalize();
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private static Vec3 spread(Vec3 direction, int count, int index, double degrees) {
        if (count <= 1 || degrees <= 0.0D) return direction;
        return rotate(direction, -degrees * 0.5D + degrees * index / (count - 1.0D));
    }

    private static Vec3 rotate(Vec3 direction, double degrees) {
        double angle = Math.toRadians(degrees);
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return new Vec3(direction.x * cosine - direction.z * sine, direction.y,
                direction.x * sine + direction.z * cosine).normalize();
    }
}
