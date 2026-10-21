package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Optional;

/**
 * Synchronous authoritative rest predicates for EchoScript hosts.
 */
public interface NpcRestQueryService {
    boolean isSleeping(ActorId npcId);

    boolean isSleepingAt(ActorId npcId, BlockPosition bedPosition);

    Optional<NpcRestAttachmentView> inspectRest(ActorId npcId);
}
