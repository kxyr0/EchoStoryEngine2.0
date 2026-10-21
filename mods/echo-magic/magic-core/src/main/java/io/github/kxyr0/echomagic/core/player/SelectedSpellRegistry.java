package io.github.kxyr0.echomagic.core.player;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.api.view.SelectedSpellView;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectedSpellRegistry {
    private final Map<MagicPlayerRef, Entry> entries = new ConcurrentHashMap<>();

    public synchronized boolean select(MagicPlayerRef player, SpellId spell, KnownSpellRegistry known) {
        if (!known.knows(player, spell)) return false;
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        if (spell.equals(entry.spell)) return true;
        entry.spell = spell;
        entry.revision++;
        return true;
    }

    public synchronized void clear(MagicPlayerRef player) {
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        if (entry.spell != null) { entry.spell = null; entry.revision++; }
    }

    public synchronized void clearIf(MagicPlayerRef player, SpellId removed) {
        if (view(player).selected().filter(removed::equals).isPresent()) clear(player);
    }

    public synchronized SelectedSpellView view(MagicPlayerRef player) {
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        return new SelectedSpellView(player, Optional.ofNullable(entry.spell), entry.revision);
    }

    public synchronized void restore(SelectedSpellView view) {
        Entry entry = new Entry();
        entry.spell = view.selected().orElse(null);
        entry.revision = view.revision();
        entries.put(view.player(), entry);
    }

    private static final class Entry { private SpellId spell; private long revision; }
}
