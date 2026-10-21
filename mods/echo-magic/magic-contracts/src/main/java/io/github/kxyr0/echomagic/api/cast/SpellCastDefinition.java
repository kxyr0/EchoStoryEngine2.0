package io.github.kxyr0.echomagic.api.cast;

import java.util.Objects;

public record SpellCastDefinition(SpellOrigin origin, SpellDirection direction, SpellHand hand,
                                  boolean swingHand, double forwardOffset) {
    public SpellCastDefinition {
        origin = Objects.requireNonNull(origin, "origin");
        direction = Objects.requireNonNull(direction, "direction");
        hand = Objects.requireNonNull(hand, "hand");
        if (!Double.isFinite(forwardOffset) || forwardOffset < 0.0D) throw new IllegalArgumentException("Invalid offset.");
    }
}

