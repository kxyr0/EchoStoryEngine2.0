package io.github.kxyr0.echovoice.forge.service;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOutcome;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOwner;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackLifecycleListener;
import io.github.kxyr0.echovoice.forge.api.VoiceService;
import io.github.kxyr0.echovoice.forge.cache.VoiceDecodedCache;
import io.github.kxyr0.echovoice.forge.catalog.FilesystemVoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetEntry;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.codec.VoiceDecoderRegistry;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodePriority;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodeQueue;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import io.github.kxyr0.echovoice.forge.playback.lane.VoiceForegroundLane;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;

/** Async decoder/cache facade; workers never access mutable Minecraft or Dialogue state. */
public final class ForgeVoiceService implements VoiceService {
    private final FilesystemVoiceAssetCatalog catalog;
    private final VoiceDecodedCache cache;
    private final VoiceDecodeQueue queue;
    private final VoiceDecoderRegistry decoders;
    private final VoiceDecodeLimits limits;
    private final VoiceForegroundLane lane;
    private final VoicePlaybackListeners listeners;
    private final VoiceLogSink diagnostics;
    private long preloadRequests;
    private long preloadHits;

    public ForgeVoiceService(FilesystemVoiceAssetCatalog catalog, VoiceDecodedCache cache, VoiceDecodeQueue queue,
            VoiceDecoderRegistry decoders, VoiceDecodeLimits limits, VoiceForegroundLane lane,
            VoicePlaybackListeners listeners, VoiceLogSink diagnostics) {
        this.catalog = catalog;
        this.cache = cache;
        this.queue = queue;
        this.decoders = decoders;
        this.limits = limits;
        this.lane = lane;
        this.listeners = listeners;
        this.diagnostics = diagnostics;
    }

    @Override
    public VoicePlaybackReceipt play(VoicePlaybackRequest request) {
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> play(request));
            return receipt(VoicePlaybackOutcome.QUEUED, "marshalled to client thread");
        }
        if (request.owner() == VoicePlaybackOwner.DEBUG && lane.snapshot().request()
                .map(active -> active.owner() == VoicePlaybackOwner.DIALOGUE).orElse(false)) {
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_DIALOGUE_FOREGROUND_CONFLICT,
                    "VOICE_FOREGROUND_LANE_OWNED_BY_DIALOGUE", request.assetKey()));
            return receipt(VoicePlaybackOutcome.REJECTED_DUPLICATE, "foreground lane owned by dialogue");
        }
        Optional<VoiceAssetEntry> entry = catalog.entry(request.assetKey());
        if (entry.isEmpty()) {
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_ASSET_NOT_FOUND,
                    "Voice asset is not indexed; Dialogue text continues.", request.assetKey()));
            return receipt(VoicePlaybackOutcome.FAILED_ASSET, "asset not found");
        }
        VoiceAssetEntry asset = entry.orElseThrow();
        VoicePlaybackReceipt queued = lane.request(request, asset.snapshot());
        if (queued.outcome() != VoicePlaybackOutcome.QUEUED) {
            return queued;
        }
        prepareCurrent(queued.playbackId(), asset);
        return queued;
    }

    @Override
    public VoicePlaybackReceipt stop(VoiceStopReason reason) {
        return lane.stop(reason);
    }

    @Override
    public void preload(VoiceAssetKey assetKey) {
        preloadRequests++;
        catalog.entry(assetKey).ifPresent(this::preparePreload);
    }

    @Override
    public void addLifecycleListener(VoicePlaybackLifecycleListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLifecycleListener(VoicePlaybackLifecycleListener listener) {
        listeners.remove(listener);
    }

    private void prepareCurrent(VoicePlaybackId playbackId, VoiceAssetEntry asset) {
        VoiceCacheKey key = new VoiceCacheKey(asset.snapshot().key(), asset.snapshot().revision());
        cache.acquire(key).ifPresentOrElse(
                lease -> lane.ready(playbackId, lease, 0, true),
                () -> decode(playbackId, asset, key));
    }

    private void decode(VoicePlaybackId playbackId, VoiceAssetEntry asset, VoiceCacheKey key) {
        long started = System.nanoTime();
        queue.submit(VoiceDecodePriority.CURRENT_LINE,
                () -> decoders.require(asset.snapshot().format()).decode(asset.path(), limits))
                .whenComplete((pcm, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure != null) {
                        fail(playbackId, asset, failure);
                        return;
                    }
                    if (!sameRevision(key)) {
                        lane.fail(playbackId, VoicePlaybackOutcome.REJECTED_STALE, "asset revision changed");
                        return;
                    }
                    cache.put(key, pcm);
                    catalog.updateDuration(key.assetKey(), key.revision(), pcm.durationMillis());
                    cache.acquire(key).ifPresent(lease -> lane.ready(playbackId, lease,
                            elapsedMillis(started), false));
                }));
    }

    private void preparePreload(VoiceAssetEntry asset) {
        VoiceCacheKey key = new VoiceCacheKey(asset.snapshot().key(), asset.snapshot().revision());
        Optional<io.github.kxyr0.echovoice.forge.cache.VoiceCacheLease> cached = cache.acquire(key);
        if (cached.isPresent()) {
            preloadHits++;
            cached.orElseThrow().close();
            return;
        }
        queue.submit(VoiceDecodePriority.PRELOAD,
                () -> decoders.require(asset.snapshot().format()).decode(asset.path(), limits))
                .whenComplete((pcm, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure == null && sameRevision(key)) {
                        cache.put(key, pcm);
                        catalog.updateDuration(key.assetKey(), key.revision(), pcm.durationMillis());
                    }
                }));
    }

    public double preloadHitRatio() {
        return preloadRequests == 0 ? 0 : (double) preloadHits / preloadRequests;
    }

    private void fail(VoicePlaybackId id, VoiceAssetEntry asset, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        VoiceDiagnosticCode code = cause instanceof VoiceDecodeException decode
                ? decode.code() : VoiceDiagnosticCode.VOICE_ASSET_DECODE_FAILED;
        diagnostics.accept(VoiceDiagnostic.error(code, "Voice decode failed: " + cause.getMessage(),
                asset.snapshot().key()));
        lane.fail(id, VoicePlaybackOutcome.FAILED_DECODE, cause.getClass().getSimpleName());
    }

    private boolean sameRevision(VoiceCacheKey key) {
        return catalog.find(key.assetKey()).map(snapshot -> snapshot.revision().equals(key.revision())).orElse(false);
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private VoicePlaybackReceipt receipt(VoicePlaybackOutcome outcome, String detail) {
        return new VoicePlaybackReceipt(VoicePlaybackId.next(), outcome, Optional.empty(), 0, detail);
    }
}
