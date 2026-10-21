package io.github.kxyr0.echoeffects.api.identity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record EffectInstanceId(UUID value) {
    public EffectInstanceId {
        value = Objects.requireNonNull(value, "value");
    }

    public static EffectInstanceId deterministic(EffectCommandId commandId, int generation) {
        String seed = commandId.value() + "#" + generation;
        return new EffectInstanceId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
    }
}
