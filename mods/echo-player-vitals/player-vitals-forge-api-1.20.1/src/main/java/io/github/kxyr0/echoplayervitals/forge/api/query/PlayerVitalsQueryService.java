package io.github.kxyr0.echoplayervitals.forge.api.query;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public query boundary for immutable Vitals snapshots.
 */
public interface PlayerVitalsQueryService {
    Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId);

    List<PlayerVitalsSnapshot> findAll();

    default Optional<PlayerVitalsSnapshot> findByAccountUuid(UUID accountUuid) {
        return find(PlayerVitalsId.fromAccountUuid(accountUuid));
    }
}
