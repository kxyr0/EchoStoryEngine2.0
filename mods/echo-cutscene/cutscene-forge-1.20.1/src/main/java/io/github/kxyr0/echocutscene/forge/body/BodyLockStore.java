package io.github.kxyr0.echocutscene.forge.body;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.forge.api.body.PlayerBodyLockService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class BodyLockStore implements PlayerBodyLockService {
    private final Map<UUID, LockedBody> locks = new HashMap<>();

    public void lock(ServerPlayer player, CutscenePlaybackId playbackId) {
        locks.put(player.getUUID(), new LockedBody(playbackId, PlayerBodySnapshot.capture(player)));
    }

    public void restore(UUID playerId, CutscenePlaybackId playbackId, PlayerBodySnapshot snapshot) {
        locks.put(playerId, new LockedBody(playbackId, snapshot));
    }

    public void unlock(ServerPlayer player) {
        locks.remove(player.getUUID());
    }

    public void clear() {
        locks.clear();
    }

    public Optional<PlayerBodySnapshot> snapshot(ServerPlayer player) {
        return snapshot(player.getUUID());
    }

    public Optional<PlayerBodySnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(locks.get(playerId)).map(LockedBody::snapshot);
    }

    @Override
    public boolean locked(ServerPlayer player) {
        return locks.containsKey(player.getUUID());
    }

    @Override
    public Optional<CutscenePlaybackId> lockOwner(ServerPlayer player) {
        return Optional.ofNullable(locks.get(player.getUUID())).map(LockedBody::playbackId);
    }

    private record LockedBody(CutscenePlaybackId playbackId, PlayerBodySnapshot snapshot) {
    }
}
