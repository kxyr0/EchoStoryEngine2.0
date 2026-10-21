package io.github.kxyr0.echoplayer.forge.client;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.sync.PlayerProfileSyncPacket;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-thread mirror for latest immutable profile packets.
 */
public final class PlayerClientMirror {
    public static final PlayerClientMirror INSTANCE = new PlayerClientMirror();
    private final Map<EchoPlayerId, PlayerProfileSyncPacket> snapshots = new ConcurrentHashMap<>();

    private PlayerClientMirror() {
    }

    public void apply(PlayerProfileSyncPacket packet) {
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(packet.accountUuid());
        snapshots.compute(playerId, (ignored, current) -> current == null
                || packet.profileRevision() >= current.profileRevision() ? packet : current);
    }

    public Optional<PlayerProfileSyncPacket> packet(EchoPlayerId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    public Optional<String> resolvedName(UUID accountUuid) {
        return packet(EchoPlayerId.fromAccountUuid(accountUuid)).map(PlayerProfileSyncPacket::resolvedName);
    }
}
