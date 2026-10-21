package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NpcSeatOccupancyRegistry {
    private final Map<String, NpcSeatOccupancy> occupancies = new LinkedHashMap<>();
    private long revision;

    public synchronized boolean reserve(NpcSeatOccupancy occupancy) {
        NpcSeatOccupancy existing = occupancies.get(occupancy.id());
        if (existing != null && !existing.npcId().equals(occupancy.npcId())) {
            return false;
        }
        occupancies.put(occupancy.id(), occupancy);
        revision++;
        return true;
    }

    public synchronized Optional<NpcSeatOccupancy> byNpc(ActorId npcId) {
        return occupancies.values().stream().filter(value -> value.npcId().equals(npcId)).findFirst();
    }

    public synchronized Optional<NpcSeatOccupancy> byTarget(String dimensionId, BlockPosition position) {
        return Optional.ofNullable(occupancies.get(dimensionId + "|" + position.x() + ","
                + position.y() + "," + position.z()));
    }

    public synchronized void releaseNpc(ActorId npcId) {
        occupancies.entrySet().removeIf(entry -> entry.getValue().npcId().equals(npcId));
        revision++;
    }

    public synchronized List<NpcSeatOccupancy> all() {
        return List.copyOf(occupancies.values());
    }

    public synchronized long revision() {
        return revision;
    }
}
