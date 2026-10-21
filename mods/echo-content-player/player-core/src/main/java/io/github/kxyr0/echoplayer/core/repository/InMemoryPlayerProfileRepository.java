package io.github.kxyr0.echoplayer.core.repository;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic repository used by unit tests and the lightweight Forge host.
 */
public final class InMemoryPlayerProfileRepository implements PlayerProfileRepository {
    private final Map<EchoPlayerId, PlayerProfileSnapshot> snapshots = new LinkedHashMap<>();
    private long repositoryRevision;

    @Override
    public synchronized Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    @Override
    public synchronized List<PlayerProfileSnapshot> findAll() {
        return List.copyOf(snapshots.values());
    }

    @Override
    public synchronized PlayerProfileSnapshot save(PlayerProfileSnapshot snapshot) {
        repositoryRevision++;
        PlayerProfileSnapshot saved = snapshot.withRevision(snapshot.profileRevision(), repositoryRevision);
        snapshots.put(saved.playerId(), saved);
        return saved;
    }

    @Override
    public synchronized int size() {
        return snapshots.size();
    }
}
