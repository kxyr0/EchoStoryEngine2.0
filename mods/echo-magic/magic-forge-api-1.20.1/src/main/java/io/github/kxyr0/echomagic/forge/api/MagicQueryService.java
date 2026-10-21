package io.github.kxyr0.echomagic.forge.api;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.api.view.KnownSpellView;
import io.github.kxyr0.echomagic.api.view.SelectedSpellView;
import io.github.kxyr0.echomagic.api.view.SpellCooldownView;

public interface MagicQueryService {
    KnownSpellView knownSpells(MagicPlayerRef player);
    SelectedSpellView selectedSpell(MagicPlayerRef player);
    SpellCooldownView cooldown(MagicPlayerRef player, SpellId spell);
}

