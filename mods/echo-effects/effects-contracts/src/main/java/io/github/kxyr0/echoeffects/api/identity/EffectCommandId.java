package io.github.kxyr0.echoeffects.api.identity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record EffectCommandId(String value) {
    public EffectCommandId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EffectCommandId must not be blank.");
        }
    }

    public static EffectCommandId deterministic(String owner, String execution, String position, EffectId effectId) {
        String seed = owner + "|" + execution + "|" + position + "|" + effectId.value();
        return new EffectCommandId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
