package io.github.kxyr0.echomagic.api.definition;

import io.github.kxyr0.echomagic.api.visual.MagicColor;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;
import java.util.Objects;

public record SpellCoreDefinition(MagicParticleStyleId particle, float size, MagicColor color,
                                  MagicColor vanillaColor) {
    public SpellCoreDefinition(MagicParticleStyleId particle, float size, MagicColor color) {
        this(particle, size, color, color);
    }

    public SpellCoreDefinition {
        particle = Objects.requireNonNull(particle, "particle");
        color = Objects.requireNonNull(color, "color");
        vanillaColor = Objects.requireNonNull(vanillaColor, "vanillaColor");
        if (!Float.isFinite(size) || size <= 0.0F) throw new IllegalArgumentException("Invalid core size.");
    }
}
