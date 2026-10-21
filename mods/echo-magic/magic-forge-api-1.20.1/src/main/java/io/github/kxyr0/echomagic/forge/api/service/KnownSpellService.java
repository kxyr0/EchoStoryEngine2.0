package io.github.kxyr0.echomagic.forge.api.service;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;

public interface KnownSpellService {
    boolean grant(MagicPlayerRef player, SpellId spell);
    boolean remove(MagicPlayerRef player, SpellId spell);
    boolean knows(MagicPlayerRef player, SpellId spell);
}

