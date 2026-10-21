package io.github.kxyr0.echovoidmagic.forge.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class VoidSwordClientParticles {
    private VoidSwordClientParticles() { }

    public static void spawn(Vec3 point) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) minecraft.level.addParticle(VoidSwordParticleEffects.options(0.20F, 5),
                point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
    }

    public static void spawnSweep(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from); double length = delta.length();
        if (length < 1.0E-6D) return;
        int samples = Math.min(5, Math.max(1, (int) Math.ceil(length / 0.12D)));
        for (int index = 1; index <= samples; index++) spawn(from.add(delta.scale(index / (double) samples)));
    }
}
