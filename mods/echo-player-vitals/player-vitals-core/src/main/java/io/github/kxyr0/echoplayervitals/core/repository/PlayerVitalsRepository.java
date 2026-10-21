package io.github.kxyr0.echoplayervitals.core.repository;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.List;
import java.util.Optional;

/**
 * Durable snapshot repository port.
 */
public interface PlayerVitalsRepository {
    Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId);

    List<PlayerVitalsSnapshot> findAll();

    PlayerVitalsSnapshot save(PlayerVitalsSnapshot snapshot);
}
