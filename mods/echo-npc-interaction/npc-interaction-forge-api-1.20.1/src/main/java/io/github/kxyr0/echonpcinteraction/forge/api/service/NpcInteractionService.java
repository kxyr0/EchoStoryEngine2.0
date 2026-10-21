package io.github.kxyr0.echonpcinteraction.forge.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;

public interface NpcInteractionService {
    NpcInteractionResult interact(NpcInteractionRequest request);

    NpcInteractionResult stop(ActorId npcId, NpcInteractionOwnerId ownerId);

    NpcInteractionResult standUp(ActorId npcId, NpcInteractionOwnerId ownerId);
}
