package io.github.kxyr0.echoitemattachments.npc;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import io.github.kxyr0.echoitemattachments.forge.api.subject.ApproachStatus;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectAccess;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;

final class NpcSubjectAccess implements AttachmentSubjectAccess {
    private static final Map<AttachmentCommandId, Long> STARTS = new LinkedHashMap<>();
    private final NpcNavigationBody body;

    NpcSubjectAccess(NpcNavigationBody body) {
        this.body = body;
    }

    @Override
    public LivingEntity entity() {
        return body.mob();
    }

    @Override
    public boolean authorises(String ownerId) {
        return !ownerId.isBlank();
    }

    @Override
    public ApproachStatus approach(AttachmentCommandId commandId, WorldPosition target,
            double acceptedDistance, int timeoutTicks, String ownerId) {
        if (!body.dimensionId().equals(target.dimensionId())) return ApproachStatus.BLOCKED;
        long now = body.level().getGameTime();
        long start = STARTS.computeIfAbsent(commandId, ignored -> now);
        if (now - start > timeoutTicks) {
            STARTS.remove(commandId);
            body.mob().getNavigation().stop();
            return ApproachStatus.BLOCKED;
        }
        double distance = body.mob().distanceToSqr(target.x(), target.y(), target.z());
        if (distance <= acceptedDistance * acceptedDistance) {
            STARTS.remove(commandId);
            body.mob().getNavigation().stop();
            return ApproachStatus.ARRIVED;
        }
        boolean accepted = body.mob().getNavigation().moveTo(target.x(), target.y(), target.z(), 1.0D);
        return accepted ? ApproachStatus.RUNNING : ApproachStatus.BLOCKED;
    }

    @Override
    public void cancelApproach(AttachmentCommandId commandId, String ownerId) {
        STARTS.remove(commandId);
        body.mob().getNavigation().stop();
    }
}
