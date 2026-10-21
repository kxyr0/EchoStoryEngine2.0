package io.github.kxyr0.echoplayer.forge.api.query;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read-only profile query boundary.
 */
public interface PlayerProfileQueryService {
    Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId);

    List<PlayerProfileSnapshot> findAll();

    default Optional<PlayerProfileSnapshot> findByAccountUuid(UUID accountUuid) {
        return find(EchoPlayerId.fromAccountUuid(accountUuid));
    }
}
