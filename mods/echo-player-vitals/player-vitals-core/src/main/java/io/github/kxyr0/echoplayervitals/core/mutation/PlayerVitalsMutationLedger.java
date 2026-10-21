package io.github.kxyr0.echoplayervitals.core.mutation;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded in-memory idempotency ledger for durable public mutations.
 */
public final class PlayerVitalsMutationLedger {
    private static final int MAX_ENTRIES_PER_PLAYER = 128;
    private final Map<PlayerVitalsId, LinkedHashMap<PlayerVitalsMutationId, PlayerVitalsSnapshot>> entries =
            new LinkedHashMap<>();

    public synchronized Optional<PlayerVitalsSnapshot> replay(PlayerVitalsId playerId,
                                                              PlayerVitalsMutationId mutationId) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(playerId, "playerId")))
                .map(playerEntries -> playerEntries.get(Objects.requireNonNull(mutationId, "mutationId")));
    }

    public synchronized void remember(PlayerVitalsId playerId, PlayerVitalsMutationId mutationId,
                                      PlayerVitalsSnapshot snapshot) {
        LinkedHashMap<PlayerVitalsMutationId, PlayerVitalsSnapshot> playerEntries = entries.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"), ignored -> new LinkedHashMap<>());
        playerEntries.put(Objects.requireNonNull(mutationId, "mutationId"),
                Objects.requireNonNull(snapshot, "snapshot"));
        while (playerEntries.size() > MAX_ENTRIES_PER_PLAYER) {
            PlayerVitalsMutationId first = playerEntries.keySet().iterator().next();
            playerEntries.remove(first);
        }
    }
}
