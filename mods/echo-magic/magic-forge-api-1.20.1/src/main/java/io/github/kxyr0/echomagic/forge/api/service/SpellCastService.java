package io.github.kxyr0.echomagic.forge.api.service;

import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.api.view.SpellCastReceipt;
import java.util.Optional;

public interface SpellCastService {
    SpellCastReceipt cast(MagicPlayerRef player, Optional<SpellId> requestedSpell, SpellCastCommandId commandId);
    void clearCooldown(MagicPlayerRef player, SpellId spellId);
}
