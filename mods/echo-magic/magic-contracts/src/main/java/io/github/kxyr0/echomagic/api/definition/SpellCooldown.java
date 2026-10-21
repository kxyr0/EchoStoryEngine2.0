package io.github.kxyr0.echomagic.api.definition;

public record SpellCooldown(long ticks) {
    public SpellCooldown { if (ticks < 0L) throw new IllegalArgumentException("Cooldown must not be negative."); }
}

