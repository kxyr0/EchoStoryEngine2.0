package io.github.kxyr0.echoplayervitals.forge.api.mutation;

import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsResult;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.UUID;

/**
 * Public mutation boundary for server-authoritative fatigue.
 */
public interface PlayerFatigueMutationService {
    PlayerVitalsResult<PlayerVitalsSnapshot> setFatigue(UUID accountUuid, PlayerVitalsMutationId mutationId,
                                                        int current);

    PlayerVitalsResult<PlayerVitalsSnapshot> addFatigue(UUID accountUuid, PlayerVitalsMutationId mutationId,
                                                        int amount);

    PlayerVitalsResult<PlayerVitalsSnapshot> reduceFatigue(UUID accountUuid, PlayerVitalsMutationId mutationId,
                                                           int amount);

    PlayerVitalsResult<PlayerVitalsSnapshot> restoreFatigue(UUID accountUuid, PlayerVitalsMutationId mutationId);
}
