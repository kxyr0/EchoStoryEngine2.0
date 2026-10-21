package io.github.kxyr0.echomagic.forge.api.service;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;

public interface SpellSelectionService {
    boolean select(MagicPlayerRef player, SpellId spell);
    void clearSelection(MagicPlayerRef player);
}

