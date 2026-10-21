package io.github.kxyr0.echoplayersleep.api.claim;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import java.util.Objects;

public record PlayerSleepStoryClaim(
        PlayerSleepSessionId sessionId,
        String triggerSubscriptionId,
        String triggerExecutionId,
        String storyExecutionId,
        PlayerSleepOwnerId rootOwner,
        PlayerSleepRevision claimRevision,
        long claimedServerTick) {
    public PlayerSleepStoryClaim {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        triggerSubscriptionId = requireText(triggerSubscriptionId, "triggerSubscriptionId");
        triggerExecutionId = requireText(triggerExecutionId, "triggerExecutionId");
        storyExecutionId = requireText(storyExecutionId, "storyExecutionId");
        rootOwner = Objects.requireNonNull(rootOwner, "rootOwner");
        claimRevision = Objects.requireNonNull(claimRevision, "claimRevision");
        if (claimedServerTick < 0) {
            throw new IllegalArgumentException("claimedServerTick must not be negative.");
        }
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
