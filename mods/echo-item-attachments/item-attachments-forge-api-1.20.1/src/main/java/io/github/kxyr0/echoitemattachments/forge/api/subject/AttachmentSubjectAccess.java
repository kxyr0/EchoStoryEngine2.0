package io.github.kxyr0.echoitemattachments.forge.api.subject;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import net.minecraft.world.entity.LivingEntity;

public interface AttachmentSubjectAccess {
    LivingEntity entity();

    boolean authorises(String ownerId);

    ApproachStatus approach(AttachmentCommandId commandId, WorldPosition target,
            double acceptedDistance, int timeoutTicks, String ownerId);

    void cancelApproach(AttachmentCommandId commandId, String ownerId);
}
