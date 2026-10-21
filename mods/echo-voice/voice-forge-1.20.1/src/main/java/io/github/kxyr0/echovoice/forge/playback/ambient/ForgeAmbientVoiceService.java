package io.github.kxyr0.echovoice.forge.playback.ambient;

import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.forge.api.ambient.AmbientVoiceService;
import io.github.kxyr0.echovoice.forge.cache.VoiceCacheLease;
import io.github.kxyr0.echovoice.forge.cache.VoiceDecodedCache;
import io.github.kxyr0.echovoice.forge.catalog.FilesystemVoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetEntry;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.codec.VoiceDecoderRegistry;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodePriority;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodeQueue;
import io.github.kxyr0.echovoice.forge.playback.lane.VoiceForegroundLane;
import io.github.kxyr0.echovoice.forge.playback.sound.VoiceSoundInstance;
import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;

/** Bounded three-voice ambient lane; distance/priority checks happen before decode. */
public final class ForgeAmbientVoiceService implements AmbientVoiceService {
    private static final int LIMIT = 3;
    private final FilesystemVoiceAssetCatalog catalog;
    private final VoiceDecodedCache cache;
    private final VoiceDecodeQueue queue;
    private final VoiceDecoderRegistry decoders;
    private final VoiceDecodeLimits limits;
    private final VoiceForegroundLane foreground;
    private final Map<String, Active> active = new HashMap<>();
    private final Set<String> pending = new HashSet<>();

    public ForgeAmbientVoiceService(FilesystemVoiceAssetCatalog catalog, VoiceDecodedCache cache,
            VoiceDecodeQueue queue, VoiceDecoderRegistry decoders, VoiceDecodeLimits limits,
            VoiceForegroundLane foreground) {
        this.catalog = catalog; this.cache = cache; this.queue = queue;
        this.decoders = decoders; this.limits = limits; this.foreground = foreground;
    }

    @Override
    public Result play(SpatialRequest request) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> play(request)); return result(Status.QUEUED, "marshalled to client thread");
        }
        stop(request.playbackToken());
        if (!nearby(minecraft, request)) {
            return result(Status.OUT_OF_RANGE, "listener outside ambient range");
        }
        if (foregroundActive()) {
            return result(Status.FOREGROUND_PRIORITY, "foreground voice has priority");
        }
        if (active.size() + pending.size() >= LIMIT) {
            return result(Status.CONCURRENCY_LIMIT, "ambient limit reached");
        }
        VoiceAssetEntry asset = catalog.entry(request.assetKey()).orElse(null);
        if (asset == null) {
            return result(Status.NO_ASSET, "asset not indexed");
        }
        VoiceCacheKey key = new VoiceCacheKey(asset.snapshot().key(), asset.snapshot().revision());
        var lease = cache.acquire(key);
        if (lease.isPresent()) {
            return start(minecraft, request, lease.orElseThrow());
        }
        pending.add(request.playbackToken());
        queue.submit(VoiceDecodePriority.CURRENT_LINE,
                () -> decoders.require(asset.snapshot().format()).decode(asset.path(), limits))
                .whenComplete((pcm, failure) -> minecraft.execute(() -> decoded(request, asset, key, pcm, failure)));
        return result(Status.QUEUED, "ambient decode queued");
    }

    @Override
    public void stop(String token) {
        pending.remove(token);
        Active removed = active.remove(token);
        if (removed != null) {
            Minecraft.getInstance().getSoundManager().stop(removed.sound());
            removed.lease().close();
        }
    }

    public void tick(Minecraft minecraft) {
        if (foregroundActive()) { Set.copyOf(active.keySet()).forEach(this::stop); return; }
        active.entrySet().removeIf(entry -> {
            if (minecraft.getSoundManager().isActive(entry.getValue().sound())) {
                return false;
            }
            entry.getValue().lease().close(); return true;
        });
    }

    public void stopAll() { Set.copyOf(active.keySet()).forEach(this::stop); pending.clear(); }
    @Override public int activeCount() { return active.size(); }

    private void decoded(SpatialRequest request, VoiceAssetEntry asset, VoiceCacheKey key,
            io.github.kxyr0.echovoice.forge.decode.DecodedPcm pcm, Throwable failure) {
        if (!pending.remove(request.playbackToken()) || failure != null || pcm == null) {
            return;
        }
        if (!catalog.find(key.assetKey()).map(value -> value.revision().equals(key.revision())).orElse(false)) {
            return;
        }
        cache.put(key, pcm);
        catalog.updateDuration(key.assetKey(), key.revision(), pcm.durationMillis());
        if (!nearby(Minecraft.getInstance(), request) || foregroundActive() || active.size() >= LIMIT) {
            return;
        }
        cache.acquire(key).ifPresent(lease -> start(Minecraft.getInstance(), request, lease));
    }

    private Result start(Minecraft minecraft, SpatialRequest request, VoiceCacheLease lease) {
        VoiceSoundInstance sound = new VoiceSoundInstance(VoicePlaybackId.next(), lease.pcm(), request.volume(),
                request.x(), request.y(), request.z(), (int) Math.ceil(request.maximumDistance()));
        try {
            minecraft.getSoundManager().play(sound);
            active.put(request.playbackToken(), new Active(sound, lease));
            return result(Status.STARTED, "ambient playback started");
        } catch (RuntimeException failure) {
            lease.close(); return result(Status.FAILED, failure.getClass().getSimpleName());
        }
    }

    private boolean nearby(Minecraft minecraft, SpatialRequest request) {
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.distanceToSqr(request.x(), request.y(), request.z())
                <= request.maximumDistance() * request.maximumDistance();
    }

    private boolean foregroundActive() {
        return foreground.snapshot().status() != io.github.kxyr0.echovoice.api.playback.VoicePlaybackStatus.IDLE;
    }
    private Result result(Status status, String detail) { return new Result(status, detail); }
    private record Active(VoiceSoundInstance sound, VoiceCacheLease lease) { }
}
