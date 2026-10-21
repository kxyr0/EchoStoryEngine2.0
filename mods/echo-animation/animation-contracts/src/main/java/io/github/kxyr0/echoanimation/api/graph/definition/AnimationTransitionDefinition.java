package io.github.kxyr0.echoanimation.api.graph.definition;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Objects;

/**
 * Explicit ordered transition inside one channel.
 */
public record AnimationTransitionDefinition(
        AnimationChannelId channelId,
        AnimationStateId fromStateId,
        String eventId,
        AnimationStateId toStateId,
        int order,
        long blendDurationTicks
) {
    public AnimationTransitionDefinition(
            AnimationChannelId channelId,
            AnimationStateId fromStateId,
            String eventId,
            AnimationStateId toStateId,
            int order
    ) {
        this(channelId, fromStateId, eventId, toStateId, order, 0L);
    }

    public AnimationTransitionDefinition {
        channelId = Objects.requireNonNull(channelId, "channelId");
        fromStateId = Objects.requireNonNull(fromStateId, "fromStateId");
        eventId = Objects.requireNonNull(eventId, "eventId").trim();
        toStateId = Objects.requireNonNull(toStateId, "toStateId");
        if (eventId.isEmpty()) {
            throw new IllegalArgumentException("eventId must be non-empty.");
        }
        if (blendDurationTicks < 0L) {
            throw new IllegalArgumentException("blendDurationTicks must not be negative.");
        }
    }
}
