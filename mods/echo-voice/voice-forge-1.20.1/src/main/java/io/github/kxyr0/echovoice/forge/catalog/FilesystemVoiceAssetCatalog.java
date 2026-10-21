package io.github.kxyr0.echovoice.forge.catalog;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetRevision;
import io.github.kxyr0.echovoice.forge.api.VoiceAssetCatalog;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Client-thread committed catalog; every exposed entry is immutable. */
public final class FilesystemVoiceAssetCatalog implements VoiceAssetCatalog {
    private final Map<VoiceAssetKey, VoiceAssetEntry> entries = new LinkedHashMap<>();
    private final Map<VoiceAssetKey, Long> revisions = new HashMap<>();

    public synchronized VoiceAssetEntry commit(VoiceAssetKey key, VoiceAssetFormat format, Path path,
            long byteSize, long durationMillis) {
        VoiceAssetRevision revision = new VoiceAssetRevision(revisions.merge(key, 1L, Long::sum));
        VoiceAssetSnapshot snapshot = new VoiceAssetSnapshot(key, revision, format,
                path.toAbsolutePath().normalize().toString(), byteSize, durationMillis);
        VoiceAssetEntry entry = new VoiceAssetEntry(snapshot, path);
        entries.put(key, entry);
        return entry;
    }

    public synchronized Optional<VoiceAssetEntry> entry(VoiceAssetKey key) {
        return Optional.ofNullable(entries.get(key));
    }

    public synchronized Optional<VoiceAssetEntry> remove(VoiceAssetKey key) {
        return Optional.ofNullable(entries.remove(key));
    }

    public synchronized void updateDuration(VoiceAssetKey key, VoiceAssetRevision revision, long durationMillis) {
        VoiceAssetEntry current = entries.get(key);
        if (current == null || !current.snapshot().revision().equals(revision)) {
            return;
        }
        VoiceAssetSnapshot old = current.snapshot();
        VoiceAssetSnapshot updated = new VoiceAssetSnapshot(old.key(), old.revision(), old.format(),
                old.sourcePath(), old.byteSize(), durationMillis);
        entries.put(key, new VoiceAssetEntry(updated, current.path()));
    }

    @Override
    public synchronized Optional<VoiceAssetSnapshot> find(VoiceAssetKey key) {
        return entry(key).map(VoiceAssetEntry::snapshot);
    }

    @Override
    public synchronized List<VoiceAssetSnapshot> assets() {
        List<VoiceAssetSnapshot> snapshots = new ArrayList<>();
        entries.values().forEach(entry -> snapshots.add(entry.snapshot()));
        snapshots.sort(Comparator.comparing(snapshot -> snapshot.key().toString()));
        return List.copyOf(snapshots);
    }
}
