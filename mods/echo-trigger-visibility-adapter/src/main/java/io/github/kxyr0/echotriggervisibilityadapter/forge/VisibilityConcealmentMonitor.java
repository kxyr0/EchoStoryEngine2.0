package io.github.kxyr0.echotriggervisibilityadapter.forge;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupEvent;
import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceBridge;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class VisibilityConcealmentMonitor {
    private static final long DEADLINE_TICKS = 10L;
    private final Map<VisibilityConcealmentLeaseId, Pending> pending = new LinkedHashMap<>();

    public void track(
            UUID player,
            VisibilityConcealmentLeaseId leaseId,
            String npcId,
            long serverTick) {
        pending.put(leaseId, new Pending(player, npcId, serverTick + DEADLINE_TICKS));
    }

    public void tick(MinecraftServer server, VisibilityServiceBridge visibility) {
        Iterator<Map.Entry<VisibilityConcealmentLeaseId, Pending>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Pending value = entry.getValue();
            var player = server.getPlayerList().getPlayer(value.player());
            if (player == null) {
                iterator.remove();
                continue;
            }
            NpcNavigationBodyLookupEvent lookup = new NpcNavigationBodyLookupEvent(
                    server, new ActorId(value.npcId()));
            MinecraftForge.EVENT_BUS.post(lookup);
            if (lookup.bodies().isEmpty()) {
                visibility.service().commitConcealment(player, entry.getKey());
                iterator.remove();
            } else if (server.getTickCount() > value.deadlineTick()) {
                visibility.service().rejectConcealment(player, entry.getKey());
                iterator.remove();
            }
        }
    }

    public int size() {
        return pending.size();
    }

    public Optional<String> npc(VisibilityConcealmentLeaseId leaseId) {
        return Optional.ofNullable(pending.get(leaseId)).map(Pending::npcId);
    }

    private record Pending(UUID player, String npcId, long deadlineTick) {
    }
}
