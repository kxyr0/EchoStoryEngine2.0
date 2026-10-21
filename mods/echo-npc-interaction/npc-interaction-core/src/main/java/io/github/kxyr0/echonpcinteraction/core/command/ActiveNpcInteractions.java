package io.github.kxyr0.echonpcinteraction.core.command;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ActiveNpcInteractions {
    private final Map<ActorId, NpcInteractionCommandId> active = new LinkedHashMap<>();

    public boolean reserve(ActorId npcId, NpcInteractionCommandId commandId) {
        if (active.containsKey(npcId)) {
            return active.get(npcId).equals(commandId);
        }
        active.put(npcId, commandId);
        return true;
    }

    public Optional<NpcInteractionCommandId> active(ActorId npcId) {
        return Optional.ofNullable(active.get(npcId));
    }

    public void release(ActorId npcId, NpcInteractionCommandId commandId) {
        active.computeIfPresent(npcId, (key, value) -> value.equals(commandId) ? null : value);
    }
}
