package io.github.kxyr0.echonpcinteraction.forge.alignment;

import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import net.minecraft.world.entity.Mob;

public final class NpcInteractionAlignment {
    public boolean align(NpcNavigationBody body, NpcInteractionApproach approach) {
        Mob mob = body.mob();
        double dx = approach.hit().hitPoint().x() - mob.getX();
        double dy = approach.hit().hitPoint().y() - mob.getEyeY();
        double dz = approach.hit().hitPoint().z() - mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);
        mob.yBodyRot = yaw;
        return true;
    }
}
