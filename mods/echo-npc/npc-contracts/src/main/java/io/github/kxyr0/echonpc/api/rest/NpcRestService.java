package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;

/**
 * Authored sleep/wake command surface.
 */
public interface NpcRestService {
    NpcResults.Sleep sleepAt(NpcSleepRequest request);

    NpcResults.Wake wakeUp(NpcWakeRequest request);

    default NpcResults.Wake wakeUp(ActorId npcId) {
        return wakeUp(new NpcWakeRequest(npcId));
    }

    default NpcResults.Wake wakeUp(ActorId npcId, ActorMutationId mutationId) {
        return wakeUp(new NpcWakeRequest(npcId, mutationId));
    }
}
