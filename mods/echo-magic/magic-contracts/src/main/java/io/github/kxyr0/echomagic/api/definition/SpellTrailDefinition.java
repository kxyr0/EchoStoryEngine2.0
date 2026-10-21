package io.github.kxyr0.echomagic.api.definition;

import io.github.kxyr0.echomagic.api.visual.MagicColor;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;
import java.util.Objects;

public record SpellTrailDefinition(MagicParticleStyleId particle, double spacing, float size, int lifetimeTicks,
                                   MagicColor color, MagicColor vanillaColor, float alphaStart, float alphaEnd) {
    public SpellTrailDefinition(MagicParticleStyleId particle, double spacing, float size, int lifetimeTicks,
                                MagicColor color, float alphaStart, float alphaEnd) {
        this(particle, spacing, size, lifetimeTicks, color, color, alphaStart, alphaEnd);
    }

    public SpellTrailDefinition {
        particle = Objects.requireNonNull(particle, "particle");
        color = Objects.requireNonNull(color, "color");
        vanillaColor = Objects.requireNonNull(vanillaColor, "vanillaColor");
        if (!Double.isFinite(spacing) || spacing <= 0.0D || size <= 0.0F || lifetimeTicks < 1
                || alphaStart < 0.0F || alphaStart > 1.0F || alphaEnd < 0.0F || alphaEnd > 1.0F) {
            throw new IllegalArgumentException("Invalid trail definition.");
        }
    }
}
