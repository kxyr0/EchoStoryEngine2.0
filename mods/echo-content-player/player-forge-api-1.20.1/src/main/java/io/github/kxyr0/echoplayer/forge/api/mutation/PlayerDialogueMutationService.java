package io.github.kxyr0.echoplayer.forge.api.mutation;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;

/**
 * Public mutation boundary for EchoScript dialogue calls such as SetPlayerName.
 */
public interface PlayerDialogueMutationService {
    PlayerProfileResult<PlayerProfileSnapshot> applyDialogueCall(EchoPlayerId playerId,
                                                                 PlayerMutationId mutationId,
                                                                 InvocationExpressionSyntax call,
                                                                 String source);
}
