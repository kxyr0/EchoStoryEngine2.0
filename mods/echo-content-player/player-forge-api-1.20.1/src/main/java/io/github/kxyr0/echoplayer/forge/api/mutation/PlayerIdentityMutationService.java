package io.github.kxyr0.echoplayer.forge.api.mutation;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;

/**
 * Public mutation boundary for Echo display name changes.
 */
public interface PlayerIdentityMutationService {
    PlayerProfileResult<PlayerProfileSnapshot> useAccountName(EchoPlayerId playerId, PlayerMutationId mutationId);

    PlayerProfileResult<PlayerProfileSnapshot> useCustomName(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                             PlayerDisplayName displayName);
}
