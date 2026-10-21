package io.github.kxyr0.echoeffects.api.time;

public record EffectTime(long ticks, float partialTicks) {
    public EffectTime {
        if (ticks < 0L) {
            throw new IllegalArgumentException("Effect time ticks must not be negative.");
        }
        if (!Float.isFinite(partialTicks) || partialTicks < 0.0F || partialTicks >= 1.0F) {
            throw new IllegalArgumentException("Partial ticks must be in [0, 1).");
        }
    }

    public double seconds() {
        return (ticks + partialTicks) / 20.0D;
    }
}
