package io.github.kxyr0.echoitemattachments.forge.runtime.source;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class HandOccupancy {
    public boolean occupied(LivingEntity entity, AttachmentPointId point) {
        return !physicalStack(entity, point).isEmpty();
    }

    private ItemStack physicalStack(LivingEntity entity, AttachmentPointId point) {
        boolean right = point.equals(AttachmentPointId.RIGHT_HAND);
        boolean mainIsRight = entity.getMainArm() == HumanoidArm.RIGHT;
        return right == mainIsRight ? entity.getMainHandItem() : entity.getOffhandItem();
    }
}
