package io.github.kxyr0.echoeffects.api.channel;

import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import java.util.Objects;

public record EffectTrack(EffectChannelId channelId, EffectFloatCurve curve, double reducerNeutral) {
    public EffectTrack {
        channelId = Objects.requireNonNull(channelId, "channelId");
        curve = Objects.requireNonNull(curve, "curve");
        if (!Double.isFinite(reducerNeutral)) {
            throw new IllegalArgumentException("Reducer neutral must be finite.");
        }
    }
}
