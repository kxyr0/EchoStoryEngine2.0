package io.github.kxyr0.echomagic.forge.client.preview;

import io.github.kxyr0.echomagic.forge.api.teleport.ShortTeleportTargetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MagicShortTeleportPreview {
    private static final DustParticleOptions EDGE =
            new DustParticleOptions(new Vector3f(0.02F, 0.38F, 0.08F), 0.75F);
    private static final DustParticleOptions CENTER =
            new DustParticleOptions(new Vector3f(0.12F, 1.0F, 0.24F), 0.95F);

    private MagicShortTeleportPreview() { }

    public static void tick(Minecraft minecraft, boolean active) {
        if (!active || minecraft.level == null || minecraft.player == null
                || minecraft.level.getGameTime() % 3L != 0L) return;
        ShortTeleportTargetResolver.find(minecraft.level, minecraft.player,
                ShortTeleportTargetResolver.DEFAULT_RANGE)
                .ifPresent(destination -> particles(minecraft, destination));
    }

    private static void particles(Minecraft minecraft, Vec3 center) {
        for (int index = 0; index < 20; index++) {
            double angle = Math.PI * 2.0D * index / 20.0D;
            double radius = 0.72D;
            minecraft.level.addParticle(EDGE, center.x + Math.cos(angle) * radius,
                    center.y + 0.06D, center.z + Math.sin(angle) * radius,
                    0.0D, 0.015D, 0.0D);
        }
        minecraft.level.addParticle(CENTER, center.x, center.y + 0.08D, center.z,
                0.0D, 0.02D, 0.0D);
        minecraft.level.addParticle(CENTER, center.x, center.y + 0.16D, center.z,
                0.0D, 0.04D, 0.0D);
    }
}
