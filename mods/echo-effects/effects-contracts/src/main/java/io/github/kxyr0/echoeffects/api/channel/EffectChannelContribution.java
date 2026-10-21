package io.github.kxyr0.echoeffects.api.channel;

import java.util.Objects;

public record EffectChannelContribution(EffectChannelId channelId, double value) {
    public EffectChannelContribution {
        channelId = Objects.requireNonNull(channelId, "channelId");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Contribution value must be finite.");
        }
    }
}
