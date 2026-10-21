package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for state and event requests.
 */
public record AnimationStateChangeResult(
        Status status,
        AnimationChannelId channelId,
        Optional<AnimationStateId> activeStateId
) {
    public AnimationStateChangeResult {
        status = Objects.requireNonNull(status, "status");
        channelId = Objects.requireNonNull(channelId, "channelId");
        activeStateId = Objects.requireNonNull(activeStateId, "activeStateId");
    }

    public enum Status {
        APPLIED,
        NO_MATCH,
        CONFLICT,
        UNKNOWN_CHANNEL,
        UNKNOWN_STATE,
        LEASE_RELEASED,
        AMBIGUOUS_TRANSITION
    }
}
