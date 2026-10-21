package io.github.kxyr0.echoplayersleep.forge.api.claim;

import io.github.kxyr0.echoplayersleep.api.claim.PlayerSleepStoryClaim;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import java.util.Optional;

public interface PlayerSleepStoryClaimService {
    PlayerSleepResult claim(
            PlayerSleepSessionId sessionId,
            String triggerSubscriptionId,
            String triggerExecutionId,
            String storyExecutionId,
            PlayerSleepOwnerId rootOwner,
            PlayerSleepCommandId commandId,
            long serverTick);

    PlayerSleepResult failUnclaimed(
            PlayerSleepSessionId sessionId,
            PlayerSleepFailure code,
            String message);

    Optional<PlayerSleepStoryClaim> claimFor(PlayerSleepSessionId sessionId);
}
