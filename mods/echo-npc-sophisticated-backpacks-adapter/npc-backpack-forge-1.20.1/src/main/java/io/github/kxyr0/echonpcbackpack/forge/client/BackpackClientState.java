package io.github.kxyr0.echonpcbackpack.forge.client;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.forge.network.BackpackSyncPacket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BackpackClientState {
    private static final Map<ActorId, BackpackClientSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private BackpackClientState() {
    }

    public static void apply(BackpackSyncPacket packet) {
        SNAPSHOTS.compute(packet.actorId(), (actor, current) -> current == null
                || packet.revision() > current.revision()
                ? new BackpackClientSnapshot(actor, packet.payload(), packet.attachmentPoint(),
                packet.status(), packet.revision()) : current);
    }

    public static Optional<BackpackClientSnapshot> get(ActorId actorId) {
        return Optional.ofNullable(SNAPSHOTS.get(actorId));
    }

    public static void clear() {
        SNAPSHOTS.clear();
        BackpackDebugState.set(Optional.empty());
    }
}
