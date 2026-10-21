package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import java.util.Objects;

public record MagicCommandBinding(MagicCommandKind kind, SpellId spellId) {
    public MagicCommandBinding {
        kind = Objects.requireNonNull(kind, "kind");
        spellId = Objects.requireNonNull(spellId, "spellId");
    }
}

