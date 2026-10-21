package io.github.kxyr0.echoik.forge.command;

import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import net.minecraft.world.entity.Mob;

/**
 * Converts Minecraft world coordinates into Echo humanoid solver coordinates.
 */
final class NpcTargetSpace {
    private static final double PIXELS_PER_BLOCK = 16.0D;
    private static final double STANDING_TARGET_Y_OFFSET_BLOCKS = 1.05D;

    Vec3 toLocal(NpcNavigationBody body, double worldX, double worldY, double worldZ) {
        Mob mob = body.mob();
        return toLocal(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), worldX, worldY, worldZ);
    }

    Vec3 toLocalStandingTarget(NpcNavigationBody body, double worldX, double worldY, double worldZ) {
        Mob mob = body.mob();
        return toLocalStandingTarget(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), worldX, worldY, worldZ);
    }

    Vec3 toLocalStandingTarget(
            double originX,
            double originY,
            double originZ,
            float bodyYaw,
            double worldX,
            double worldY,
            double worldZ
    ) {
        return toLocal(originX, originY, originZ, bodyYaw, worldX, worldY + STANDING_TARGET_Y_OFFSET_BLOCKS, worldZ);
    }

    Vec3 toLocal(
            double originX,
            double originY,
            double originZ,
            float bodyYaw,
            double worldX,
            double worldY,
            double worldZ
    ) {
        double dx = worldX - originX;
        double dy = worldY - originY;
        double dz = worldZ - originZ;
        double yaw = Math.toRadians(-bodyYaw);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double localX = dx * cos - dz * sin;
        double localZ = dx * sin + dz * cos;
        return new Vec3(localX * PIXELS_PER_BLOCK, dy * PIXELS_PER_BLOCK, localZ * PIXELS_PER_BLOCK);
    }
}
