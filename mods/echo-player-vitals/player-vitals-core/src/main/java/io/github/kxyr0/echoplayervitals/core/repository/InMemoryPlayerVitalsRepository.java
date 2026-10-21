package io.github.kxyr0.echoplayervitals.core.repository;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Non-durable repository used before SavedData is available and in tests.
 */
public final class InMemoryPlayerVitalsRepository implements PlayerVitalsRepository {
    private final Map<PlayerVitalsId, PlayerVitalsSnapshot> snapshots = new LinkedHashMap<>();

    @Override
    public synchronized Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    @Override
    public synchronized List<PlayerVitalsSnapshot> findAll() {
        return new ArrayList<>(snapshots.values());
    }

    @Override
    public synchronized PlayerVitalsSnapshot save(PlayerVitalsSnapshot snapshot) {
        snapshots.put(snapshot.playerId(), snapshot);
        return snapshot;
    }
}
