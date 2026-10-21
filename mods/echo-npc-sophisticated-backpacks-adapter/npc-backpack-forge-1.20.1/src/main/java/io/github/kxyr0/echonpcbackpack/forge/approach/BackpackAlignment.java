package io.github.kxyr0.echonpcbackpack.forge.approach;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class BackpackAlignment {
    private static final float TOLERANCE_DEGREES = 6.0F;

    public boolean align(Mob mob, BlockPos target) {
        Vec3 center = Vec3.atCenterOf(target);
        mob.lookAt(EntityAnchorArgument.Anchor.EYES, center);
        double dx = center.x - mob.getX();
        double dz = center.z - mob.getZ();
        float expected = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float difference = Math.abs(Mth.wrapDegrees(mob.getYHeadRot() - expected));
        return difference <= TOLERANCE_DEGREES;
    }
}
