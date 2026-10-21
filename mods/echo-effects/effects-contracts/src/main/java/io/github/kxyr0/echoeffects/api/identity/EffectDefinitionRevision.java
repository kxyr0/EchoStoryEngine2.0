package io.github.kxyr0.echoeffects.api.identity;

public record EffectDefinitionRevision(long value) {
    public EffectDefinitionRevision {
        if (value <= 0L) {
            throw new IllegalArgumentException("Definition revision must be positive.");
        }
    }

    public static EffectDefinitionRevision initial() {
        return new EffectDefinitionRevision(1L);
    }
}
