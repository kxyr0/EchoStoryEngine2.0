package io.github.kxyr0.echomagic.api.view;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.Objects;
import java.util.Optional;

public record SelectedSpellView(MagicPlayerRef player, Optional<SpellId> selected, long revision) {
    public SelectedSpellView {
        player = Objects.requireNonNull(player, "player");
        selected = Objects.requireNonNull(selected, "selected");
        if (revision < 0L) throw new IllegalArgumentException("Revision must not be negative.");
    }
}

