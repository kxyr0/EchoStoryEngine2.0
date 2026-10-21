package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import net.minecraft.server.level.ServerPlayer;

public interface VisibilityService {
    VisibilityServiceResult arm(ServerPlayer player, VisibilityWatchId watchId, String ownerLineage);

    VisibilityServiceResult disarm(ServerPlayer player, VisibilityWatchId watchId);

    VisibilityServiceResult assertStillOutOfView(ServerPlayer player, VisibilityWatchId watchId);

    VisibilityServiceResult claimConcealment(
            ServerPlayer player,
            VisibilityConcealmentLeaseId leaseId,
            String claimantId);

    VisibilityServiceResult commitConcealment(ServerPlayer player, VisibilityConcealmentLeaseId leaseId);

    VisibilityServiceResult rejectConcealment(ServerPlayer player, VisibilityConcealmentLeaseId leaseId);
}
