package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.identity.EffectInstanceId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import java.util.Objects;

public record EffectRuntimeTimelineEvent(
        EffectTargetRef target,
        EffectInstanceId instanceId,
        EffectTimelineEvent event
) {
    public EffectRuntimeTimelineEvent {
        target = Objects.requireNonNull(target, "target");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        event = Objects.requireNonNull(event, "event");
    }
}
