package io.github.kxyr0.echomagic.core.player;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.api.view.KnownSpellView;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

public final class KnownSpellRegistry {
    private final Map<MagicPlayerRef, Entry> entries = new ConcurrentHashMap<>();
    private final IntSupplier maximum;

    public KnownSpellRegistry(int maximum) { this(() -> maximum); }
    public KnownSpellRegistry(IntSupplier maximum) { this.maximum = maximum; }

    public synchronized boolean grant(MagicPlayerRef player, SpellId spell) {
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        if (entry.spells.contains(spell)) return false;
        if (entry.spells.size() >= maximum.getAsInt()) throw new IllegalStateException("Known spell limit reached.");
        entry.spells.add(spell);
        entry.revision++;
        return true;
    }

    public synchronized boolean remove(MagicPlayerRef player, SpellId spell) {
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        boolean removed = entry.spells.remove(spell);
        if (removed) entry.revision++;
        return removed;
    }

    public synchronized boolean knows(MagicPlayerRef player, SpellId spell) { return view(player).spells().contains(spell); }

    public synchronized KnownSpellView view(MagicPlayerRef player) {
        Entry entry = entries.computeIfAbsent(player, ignored -> new Entry());
        return new KnownSpellView(player, Set.copyOf(entry.spells), entry.revision);
    }

    public synchronized void restore(KnownSpellView view) {
        Entry entry = new Entry();
        entry.spells.addAll(view.spells());
        entry.revision = view.revision();
        entries.put(view.player(), entry);
    }

    private static final class Entry { private final Set<SpellId> spells = new LinkedHashSet<>(); private long revision; }
}
