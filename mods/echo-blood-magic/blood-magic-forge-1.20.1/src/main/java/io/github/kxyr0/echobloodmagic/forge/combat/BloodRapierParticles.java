package io.github.kxyr0.echobloodmagic.forge.combat;

import io.github.kxyr0.echobloodmagic.forge.BloodMagicParticles;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class BloodRapierParticles {
    private BloodRapierParticles() { }

    static void summon(ServerPlayer player) {
        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(0.55D)).add(0.0D, -0.35D, 0.0D);
        for (int index = 0; index < 18; index++) {
            double angle = index * Math.PI * 2.0D / 18.0D;
            Vec3 point = center.add(Math.cos(angle) * 0.42D, (index % 6) * 0.08D - 0.2D,
                    Math.sin(angle) * 0.42D);
            point(player.serverLevel(), index % 3 == 0 ? bright(0.2F, 10) : dark(0.16F, 9), point);
        }
    }

    static void mark(ServerLevel level, LivingEntity target, boolean pulse) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.62D, 0.0D);
        double radius = Math.max(0.36D, target.getBbWidth() * 0.65D);
        int points = pulse ? 12 : 8;
        for (int index = 0; index < points; index++) {
            double angle = index * Math.PI * 2.0D / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            point(level, pulse ? bright(0.19F, 9) : dark(0.15F, 7), point);
        }
        if (pulse) {
            for (int index = -2; index <= 2; index++) {
                point(level, bright(0.16F, 8), center.add(0.0D, index * 0.16D, 0.0D));
            }
        }
    }

    static void hit(ServerLevel level, LivingEntity target, Vec3 look) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        Vec3 right = horizontal.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(horizontal.z, 0.0D, -horizontal.x).normalize();
        for (int index = -4; index <= 4; index++) {
            Vec3 point = center.add(right.scale(index * 0.12D)).add(0.0D, index * 0.065D, 0.0D);
            point(level, bright(0.2F, 7), point);
        }
    }

    static void link(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        Vec3 from = caster.getEyePosition().add(0.0D, -0.2D, 0.0D);
        Vec3 to = target.getBoundingBox().getCenter();
        for (int index = 0; index <= 14; index++) {
            Vec3 point = from.lerp(to, index / 14.0D);
            point(level, index % 3 == 0 ? bright(0.11F, 6) : dark(0.09F, 5), point);
        }
    }

    static void needle(ServerLevel level, Vec3 from, Vec3 to) {
        for (int index = 0; index <= 24; index++) {
            Vec3 point = from.lerp(to, index / 24.0D);
            point(level, index % 4 == 0 ? bright(0.13F, 7) : dark(0.085F, 6), point);
        }
    }

    private static MagicBillboardParticleOptions bright(float size, int lifetime) {
        return new MagicBillboardParticleOptions(BloodMagicParticles.BLOOD_CIRCLE.get(),
                1.0F, 0.42F, 0.42F, size, lifetime, 0.96F, 0.0F);
    }

    private static MagicBillboardParticleOptions dark(float size, int lifetime) {
        return new MagicBillboardParticleOptions(BloodMagicParticles.BLOOD_CIRCLE.get(),
                0.48F, 0.04F, 0.05F, size, lifetime, 0.88F, 0.0F);
    }

    private static void point(ServerLevel level, MagicBillboardParticleOptions particle, Vec3 point) {
        level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
