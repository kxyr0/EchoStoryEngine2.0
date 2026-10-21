package io.github.kxyr0.echoplayer.forge.api.mutation;

import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;

/**
 * Public mutation boundary for base appearance changes.
 */
public interface PlayerAppearanceMutationService {
    PlayerProfileResult<PlayerProfileSnapshot> useAccountSkin(EchoPlayerId playerId, PlayerMutationId mutationId);

    PlayerProfileResult<PlayerProfileSnapshot> useResourceSkin(EchoPlayerId playerId, PlayerMutationId mutationId,
                                                               PlayerSkinId skinId, PlayerSkinModel model);
}
