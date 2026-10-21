package io.github.kxyr0.echoplayer.forge.api.mutation;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;

/**
 * Public mutation boundary for Spark.
 */
public interface PlayerSparkMutationService {
    PlayerProfileResult<PlayerProfileSnapshot> set(EchoPlayerId playerId, PlayerMutationId mutationId, int current);

    PlayerProfileResult<PlayerProfileSnapshot> restore(EchoPlayerId playerId, PlayerMutationId mutationId, int amount);

    PlayerProfileResult<PlayerProfileSnapshot> spend(EchoPlayerId playerId, PlayerMutationId mutationId, int amount);

    PlayerProfileResult<PlayerProfileSnapshot> setMaximum(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                          int maximum);
}
