package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.cast.SpellDirection;
import io.github.kxyr0.echomagic.api.cast.SpellOrigin;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.core.gesture.composition.MagicGestureComposition;
import io.github.kxyr0.echomagic.forge.cast.GestureCastContext;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class GestureCastContextFactory {
    private static final double MAXIMUM_AUTO_AIM_RANGE = 24.0D;
    private static final double MINIMUM_FORWARD_COSINE = 0.30D;

    private GestureCastContextFactory() { }

    static GestureCastContext create(ForgeMagicServices services, ServerPlayer player, SpellDefinition definition,
                                     MagicGestureComposition composition,
                                     Optional<LivingEntity> linkedTarget) {
        MagicGestureModifier modifier = composition.modifier();
        if (composition.technique() == MagicTechnique.HOOK) {
            Vec3 origin = player.getEyePosition();
            LivingEntity target = linkedTarget.orElseThrow();
            Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            return GestureCastContext.seal(origin, targetPoint.subtract(origin));
        }
        Vec3 direction = direction(player, definition);
        boolean line = composition.technique() == MagicTechnique.LINE;
        if (line) direction = autoAim(services, player, definition).orElse(direction);
        if (modifier == MagicGestureModifier.NONE) {
            return line ? GestureCastContext.aimed(direction) : GestureCastContext.immediate();
        }
        if (modifier == MagicGestureModifier.SPLIT) {
            return line ? GestureCastContext.splitCast().withDirection(direction) : GestureCastContext.splitCast();
        }
        if (definition.technique() == MagicTechnique.CIRCLE) {
            Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
            if (horizontal.lengthSqr() <= 1.0E-8D) horizontal = direction(player, definition);
            Vec3 origin = player.position().add(horizontal.normalize().scale(3.0D)).add(0.0D, 0.25D, 0.0D);
            return GestureCastContext.seal(origin, horizontal);
        }
        Vec3 origin = definition.cast().origin() == SpellOrigin.PLAYER_EYES
                ? player.getEyePosition() : player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        return GestureCastContext.delayed(origin, direction, 40L);
    }

    private static Optional<Vec3> autoAim(ForgeMagicServices services, ServerPlayer player,
                                          SpellDefinition definition) {
        double range = Math.min(MAXIMUM_AUTO_AIM_RANGE, definition.projectile().range());
        Vec3 eye = player.getEyePosition(); Vec3 look = player.getLookAngle().normalize();
        List<Vec3> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(range), target -> services.targetFilter()
                                .valid(player, target, false) && player.hasLineOfSight(target))
                .stream().map(GestureCastContextFactory::center).toList();
        return nearestAim(eye, look, targets, range);
    }

    static Optional<Vec3> nearestAim(Vec3 eye, Vec3 look, List<Vec3> targets, double range) {
        double maximumDistanceSquared = range * range;
        return targets.stream().filter(point -> {
            Vec3 delta = point.subtract(eye);
            return delta.lengthSqr() > 1.0E-8D && delta.lengthSqr() <= maximumDistanceSquared
                    && delta.normalize().dot(look) >= MINIMUM_FORWARD_COSINE;
        }).min(Comparator.comparingDouble(eye::distanceToSqr)).map(point -> point.subtract(eye).normalize());
    }

    private static Vec3 center(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 direction(ServerPlayer player, SpellDefinition definition) {
        Vec3 look = player.getLookAngle().normalize();
        if (definition.cast().direction() == SpellDirection.PLAYER_LOOK) return look;
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-8D) return horizontal.normalize();
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }
}
