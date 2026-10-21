package io.github.kxyr0.echomagic.api.view;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import java.util.Objects;

public record SpellCooldownView(SpellId spellId, long remainingTicks, long revision) {
    public SpellCooldownView {
        spellId = Objects.requireNonNull(spellId, "spellId");
        if (remainingTicks < 0L || revision < 0L) throw new IllegalArgumentException("Negative cooldown view value.");
    }
}

