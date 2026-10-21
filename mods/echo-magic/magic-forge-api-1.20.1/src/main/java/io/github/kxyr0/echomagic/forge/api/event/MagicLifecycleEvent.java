package io.github.kxyr0.echomagic.forge.api.event;

import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class MagicLifecycleEvent extends Event {
    private final MagicPlayerRef player;
    private final SpellId spellId;
    private final SpellCastId castId;
    private boolean handled;

    public MagicLifecycleEvent(MagicPlayerRef player, SpellId spellId, SpellCastId castId) {
        this.player = Objects.requireNonNull(player, "player");
        this.spellId = Objects.requireNonNull(spellId, "spellId");
        this.castId = Objects.requireNonNull(castId, "castId");
    }
    public MagicPlayerRef player() { return player; }
    public SpellId spellId() { return spellId; }
    public SpellCastId castId() { return castId; }
    public boolean handled() { return handled; }
    public void markHandled() { handled = true; }
}
