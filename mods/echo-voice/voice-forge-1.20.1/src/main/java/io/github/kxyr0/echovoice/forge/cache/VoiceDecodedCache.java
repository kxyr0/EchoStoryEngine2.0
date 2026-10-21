package io.github.kxyr0.echovoice.forge.cache;

import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Bounded byte-weighted LRU cache with active/preload leases. */
public final class VoiceDecodedCache {
    private final long maximumBytes;
    private final Map<VoiceCacheKey, Entry> entries = new LinkedHashMap<>(16, 0.75F, true);
    private long bytes;
    private long hits;
    private long misses;

    public VoiceDecodedCache(long maximumBytes) {
        this.maximumBytes = maximumBytes;
    }

    public synchronized void put(VoiceCacheKey key, DecodedPcm pcm) {
        Entry previous = entries.put(key, new Entry(pcm));
        if (previous != null) {
            bytes -= previous.pcm.byteSize();
        }
        bytes += pcm.byteSize();
        evict();
    }

    public synchronized Optional<VoiceCacheLease> acquire(VoiceCacheKey key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            misses++;
            return Optional.empty();
        }
        hits++;
        entry.leases++;
        return Optional.of(new VoiceCacheLease(this, key, entry.pcm));
    }

    synchronized void release(VoiceCacheKey key) {
        Entry entry = entries.get(key);
        if (entry != null && entry.leases > 0) {
            entry.leases--;
        }
        evict();
    }

    public synchronized long bytes() {
        return bytes;
    }

    public synchronized int entryCount() {
        return entries.size();
    }

    public synchronized double hitRatio() {
        long attempts = hits + misses;
        return attempts == 0 ? 0.0 : (double) hits / attempts;
    }

    private void evict() {
        boolean removed = true;
        while (bytes > maximumBytes && removed) {
            removed = false;
            Iterator<Map.Entry<VoiceCacheKey, Entry>> iterator = entries.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next().getValue();
                if (entry.leases == 0) {
                    bytes -= entry.pcm.byteSize();
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        }
    }

    private static final class Entry {
        private final DecodedPcm pcm;
        private int leases;

        private Entry(DecodedPcm pcm) {
            this.pcm = pcm;
        }
    }
}
