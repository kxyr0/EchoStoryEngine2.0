package io.github.kxyr0.echoeffects.api.channel;

import io.github.kxyr0.echoeffects.api.time.EffectTime;
import java.util.Map;
import java.util.Objects;

public record EffectTimelineEvent(EffectTime time, EffectChannelId channelId, Map<String, String> payload) {
    public EffectTimelineEvent {
        time = Objects.requireNonNull(time, "time");
        channelId = Objects.requireNonNull(channelId, "channelId");
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
    }
}
