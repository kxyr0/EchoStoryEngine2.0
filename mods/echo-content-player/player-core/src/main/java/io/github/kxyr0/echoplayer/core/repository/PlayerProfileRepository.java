package io.github.kxyr0.echoplayer.core.repository;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.List;
import java.util.Optional;

/**
 * Server-authoritative durable profile repository boundary.
 */
public interface PlayerProfileRepository {
    Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId);

    List<PlayerProfileSnapshot> findAll();

    PlayerProfileSnapshot save(PlayerProfileSnapshot snapshot);

    int size();
}
