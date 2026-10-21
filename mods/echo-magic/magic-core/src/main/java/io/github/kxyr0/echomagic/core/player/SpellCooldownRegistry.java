package io.github.kxyr0.echomagic.core.player;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.api.view.SpellCooldownView;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellCooldownRegistry {
    private final Map<MagicPlayerRef, Map<SpellId, Long>> readyTicks = new ConcurrentHashMap<>();
    private long revision;

    public synchronized boolean ready(MagicPlayerRef player, SpellId spell, long tick) {
        return readyTick(player, spell) <= tick;
    }

    public synchronized void begin(MagicPlayerRef player, SpellId spell, long tick, long duration) {
        readyTicks.computeIfAbsent(player, ignored -> new HashMap<>()).put(spell, tick + duration);
        revision++;
    }

    public synchronized void clear(MagicPlayerRef player, SpellId spell) {
        Map<SpellId, Long> spells = readyTicks.get(player);
        if (spells != null && spells.remove(spell) != null) revision++;
    }

    public synchronized SpellCooldownView view(MagicPlayerRef player, SpellId spell, long tick) {
        return new SpellCooldownView(spell, Math.max(0L, readyTick(player, spell) - tick), revision);
    }

    public synchronized Map<SpellId, Long> readyTicks(MagicPlayerRef player) {
        return Map.copyOf(readyTicks.getOrDefault(player, Map.of()));
    }

    public synchronized Map<SpellId, Long> remainingTicks(MagicPlayerRef player, long tick) {
        Map<SpellId, Long> remaining = new HashMap<>();
        readyTicks.getOrDefault(player, Map.of()).forEach((spell, readyTick) -> {
            long duration = Math.max(0L, readyTick - tick);
            if (duration > 0L) remaining.put(spell, duration);
        });
        return Map.copyOf(remaining);
    }

    public synchronized void restore(MagicPlayerRef player, Map<SpellId, Long> values) {
        readyTicks.put(player, new HashMap<>(values));
        revision++;
    }

    public synchronized void restoreRemaining(MagicPlayerRef player, Map<SpellId, Long> values, long tick) {
        Map<SpellId, Long> restored = new HashMap<>();
        values.forEach((spell, remaining) -> {
            if (remaining > 0L) restored.put(spell, tick + remaining);
        });
        readyTicks.put(player, restored);
        revision++;
    }

    private long readyTick(MagicPlayerRef player, SpellId spell) {
        return readyTicks.getOrDefault(player, Map.of()).getOrDefault(spell, 0L);
    }
}
