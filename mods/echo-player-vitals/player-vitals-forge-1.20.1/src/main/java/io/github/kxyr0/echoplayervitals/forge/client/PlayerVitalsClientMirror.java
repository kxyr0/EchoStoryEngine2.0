package io.github.kxyr0.echoplayervitals.forge.client;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.forge.api.client.PlayerVitalsClientSnapshotView;
import io.github.kxyr0.echoplayervitals.forge.sync.PlayerVitalsSyncPacket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

/**
 * Client-side immutable latest Vitals snapshots.
 */
public final class PlayerVitalsClientMirror implements PlayerVitalsClientSnapshotView {
    public static final PlayerVitalsClientMirror INSTANCE = new PlayerVitalsClientMirror();
    private final Map<PlayerVitalsId, PlayerVitalsSnapshot> snapshots = new ConcurrentHashMap<>();

    private PlayerVitalsClientMirror() {
    }

    public void apply(PlayerVitalsSyncPacket packet) {
        PlayerVitalsSnapshot incoming = packet.toSnapshot();
        snapshots.compute(incoming.playerId(), (ignored, current) -> current == null
                || incoming.revision().compareTo(current.revision()) >= 0 ? incoming : current);
    }

    @Override
    public Optional<PlayerVitalsSnapshot> local() {
        if (Minecraft.getInstance().player == null) {
            return Optional.empty();
        }
        return find(PlayerVitalsId.fromAccountUuid(Minecraft.getInstance().player.getUUID()));
    }

    @Override
    public Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }
}
