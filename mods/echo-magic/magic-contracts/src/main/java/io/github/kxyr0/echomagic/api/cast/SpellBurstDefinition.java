package io.github.kxyr0.echomagic.api.cast;

public record SpellBurstDefinition(int count, long intervalTicks, double spreadDegrees) {
    public SpellBurstDefinition(int count, long intervalTicks) { this(count, intervalTicks, 0.0D); }

    public SpellBurstDefinition {
        if (count < 1) throw new IllegalArgumentException("Burst count must be positive.");
        if (intervalTicks < 0L) throw new IllegalArgumentException("Burst interval must not be negative.");
        if (!Double.isFinite(spreadDegrees) || spreadDegrees < 0.0D || spreadDegrees > 360.0D) {
            throw new IllegalArgumentException("Burst spread must be between 0 and 360 degrees.");
        }
    }
}
