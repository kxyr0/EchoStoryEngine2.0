package io.github.kxyr0.echoaudio.asset;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.pcm.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Serialized worker: all scan/hash/decode/mono work is asynchronous; bounded LRU with revision-pinned leases. */
public final class AudioCatalog implements AutoCloseable {
    public record Ready(AudioAssetDescriptor asset, AudioPcm local, AudioPcm spatial) {
        public long bytes() { return local.size() + (spatial == local ? 0 : spatial.size()); }
    }
    private final Path root;
    private final AudioDecoder decoder;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "echo-audio-decode"); thread.setDaemon(true); return thread;
    });
    private final Map<AudioResourceId, Ready> cache = new LinkedHashMap<>(16, .75f, true);
    private volatile Map<AudioResourceId, AudioAssetDescriptor> assets = Map.of();
    private volatile List<AudioFailure> problems = List.of();
    private volatile Map<AudioResourceId, AudioFailure> rejected = Map.of();
    private final long budget;
    public AudioCatalog(Path root, AudioDecoder decoder, long budget) {
        this.root = root; this.decoder = decoder; this.budget = budget;
    }
    public List<AudioAssetDescriptor> assets() { return List.copyOf(assets.values()); }
    public List<AudioFailure> problems() { return problems; }
    public CompletableFuture<Void> reload() {
        return CompletableFuture.runAsync(() -> {
            try {
                var scan = new AudioAssetScanner().scan(root);
                Map<AudioResourceId, AudioAssetDescriptor> replacement = new LinkedHashMap<>();
                List<AudioFailure> errors = new ArrayList<>(scan.problems());
                for (var asset : scan.assets()) {
                    var previous = assets.get(asset.id());
                    if (previous != null && !previous.revision().equals(asset.revision())) {
                        try { put(decode(asset)); }
                        catch (AudioFailure failure) { errors.add(failure); replacement.put(previous.id(), previous); continue; }
                    }
                    replacement.put(asset.id(), asset);
                }
                assets = Map.copyOf(replacement); problems = List.copyOf(errors); rejected = scan.rejected();
                cache.keySet().retainAll(assets.keySet());
            } catch (Exception failure) {
                problems = List.of(new AudioFailure("AUDIO_DECODE_FAILED", "Catalog reload: " + failure.getMessage()));
            }
        }, worker);
    }
    public CompletableFuture<Ready> preload(AudioResourceId id) {
        return CompletableFuture.supplyAsync(() -> {
            var asset = assets.get(id);
            if (asset == null) throw rejected.getOrDefault(id, new AudioFailure("AUDIO_ASSET_NOT_FOUND", id.value()));
            Ready ready = cache.get(id);
            if (ready != null && ready.asset().revision().equals(asset.revision())) return ready;
            ready = decode(asset); put(ready); return ready;
        }, worker);
    }
    private Ready decode(AudioAssetDescriptor asset) {
        try {
            if (!AudioAssetScanner.revision(asset.path()).equals(asset.revision())) {
                throw new IllegalArgumentException("Asset changed before decode; reload required");
            }
            AudioPcm pcm = decoder.decode(asset);
            if (!AudioAssetScanner.revision(asset.path()).equals(asset.revision())) {
                throw new IllegalArgumentException("Asset changed during decode; reload required");
            }
            Ready ready = new Ready(asset, pcm, pcm.mono());
            if (ready.bytes() > budget) throw new IllegalArgumentException("PCM exceeds cache budget");
            return ready;
        } catch (Exception failure) {
            throw new AudioFailure("AUDIO_DECODE_FAILED", asset.id().value() + " " + asset.path()
                    + " decoder=" + asset.format() + " reason=" + failure.getMessage());
        }
    }
    private void put(Ready ready) {
        cache.put(ready.asset().id(), ready);
        long size = cache.values().stream().mapToLong(Ready::bytes).sum();
        var iterator = cache.values().iterator();
        while (size > budget && iterator.hasNext()) { size -= iterator.next().bytes(); iterator.remove(); }
    }
    public CompletableFuture<List<String>> status() {
        return CompletableFuture.supplyAsync(() -> assets.values().stream()
                .sorted(Comparator.comparing(a -> a.id().value())).map(asset -> {
                    Ready ready = cache.get(asset.id());
                    return asset.id().value() + " format=" + asset.format() + " durationMs="
                            + (ready == null ? "unknown" : ready.local().durationMillis())
                            + " cached=" + (ready != null) + " source=" + asset.path();
                }).toList(), worker);
    }
    @Override public void close() { worker.shutdownNow(); }
}
