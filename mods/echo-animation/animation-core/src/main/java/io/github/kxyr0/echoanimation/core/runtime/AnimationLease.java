package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import java.util.Objects;

/**
 * Immutable proof of current channel ownership.
 */
public record AnimationLease(
        AnimationLeaseId id,
        AnimationOwnerId ownerId,
        AnimationChannelId channelId
) {
    public AnimationLease {
        id = Objects.requireNonNull(id, "id");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        channelId = Objects.requireNonNull(channelId, "channelId");
    }
}
