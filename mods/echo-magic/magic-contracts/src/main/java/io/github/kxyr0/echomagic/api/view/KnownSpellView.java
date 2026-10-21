package io.github.kxyr0.echomagic.api.view;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.Objects;
import java.util.Set;

public record KnownSpellView(MagicPlayerRef player, Set<SpellId> spells, long revision) {
    public KnownSpellView {
        player = Objects.requireNonNull(player, "player");
        spells = Set.copyOf(Objects.requireNonNull(spells, "spells"));
        if (revision < 0L) throw new IllegalArgumentException("Revision must not be negative.");
    }
}

