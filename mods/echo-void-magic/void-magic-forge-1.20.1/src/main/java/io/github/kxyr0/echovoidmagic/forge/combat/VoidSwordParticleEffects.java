package io.github.kxyr0.echovoidmagic.forge.combat;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleOptions;
import io.github.kxyr0.echovoidmagic.forge.VoidMagicParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VoidSwordParticleEffects {
    private static final ResourceLocation VOID_SWORD = ResourceLocation.fromNamespaceAndPath("jdh", "void_sword");
    private VoidSwordParticleEffects() { }

    public static boolean holdsVoidSword(Player player) {
        return VOID_SWORD.equals(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()));
    }

    public static List<Vec3> bladePoints(Vec3 eye, Vec3 forwardValue, Vec3 upValue, double progress) {
        Vec3 forward = forwardValue.normalize(); Vec3 up = upValue.normalize();
        Vec3 right = forward.cross(up).normalize();
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        double eased = clamped * clamped * (3.0D - 2.0D * clamped);
        double angle = 0.85D + 2.35D * eased;
        Vec3 slash = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).normalize();
        Vec3 grip = eye.add(forward.scale(0.30D)).add(up.scale(-0.42D)).add(slash.scale(0.18D));
        Vec3 blade = slash.scale(0.84D).add(forward.scale(0.54D)).normalize();
        List<Vec3> points = new ArrayList<>();
        for (int index = 0; index < 6; index++) points.add(grip.add(blade.scale(0.24D + index * 0.16D)));
        return List.copyOf(points);
    }

    public static void spawnHit(ServerLevel level, LivingEntity target, Player attacker) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 forward = attacker.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        if (forward.lengthSqr() < 1.0E-6D) forward = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        for (int index = -4; index <= 4; index++) {
            double offset = index * 0.13D;
            Vec3 point = center.add(right.scale(offset)).add(0.0D, -offset * 0.55D, 0.0D)
                    .add(forward.scale(-0.08D));
            level.sendParticles(options(0.24F, 7), point.x, point.y, point.z,
                    1, 0.015D, 0.015D, 0.015D, 0.0D);
        }
    }

    public static MagicBillboardParticleOptions options(float size, int lifetime) {
        return new MagicBillboardParticleOptions(VoidMagicParticles.VOID_CIRCLE.get(),
                0.18F, 1.0F, 0.22F, size, lifetime, 0.90F, 0.0F);
    }
}
