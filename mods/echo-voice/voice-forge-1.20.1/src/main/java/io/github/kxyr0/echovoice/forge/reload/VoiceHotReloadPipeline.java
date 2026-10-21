package io.github.kxyr0.echovoice.forge.reload;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import io.github.kxyr0.echovoice.forge.cache.VoiceDecodedCache;
import io.github.kxyr0.echovoice.forge.catalog.FilesystemVoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetEntry;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetPathResolver;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.codec.VoiceDecoderRegistry;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodePriority;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodeQueue;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import io.github.kxyr0.echovoice.forge.reload.stability.VoiceFileStabilityProbe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Debounced stable-file decode, validation and atomic client-thread catalog commit. */
public final class VoiceHotReloadPipeline {
    private final VoiceAssetPathResolver resolver;
    private final FilesystemVoiceAssetCatalog catalog;
    private final VoiceDecodedCache cache;
    private final VoiceDecodeQueue queue;
    private final VoiceDecoderRegistry decoders;
    private final VoiceDecodeLimits limits;
    private final VoiceFileStabilityProbe stability;
    private final VoiceLogSink diagnostics;
    private volatile long lastLatencyMillis;

    public VoiceHotReloadPipeline(VoiceAssetPathResolver resolver, FilesystemVoiceAssetCatalog catalog,
            VoiceDecodedCache cache, VoiceDecodeQueue queue, VoiceDecoderRegistry decoders, VoiceDecodeLimits limits,
            VoiceFileStabilityProbe stability, VoiceLogSink diagnostics) {
        this.resolver = resolver;
        this.catalog = catalog;
        this.cache = cache;
        this.queue = queue;
        this.decoders = decoders;
        this.limits = limits;
        this.stability = stability;
        this.diagnostics = diagnostics;
    }

    public void reload(VoiceAssetKey key) {
        List<Path> candidates = resolver.supportedFiles(key);
        if (candidates.isEmpty()) {
            catalog.remove(key);
            return;
        }
        if (candidates.size() > 1) {
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_HOT_RELOAD_AMBIGUOUS,
                    "VOICE_ASSET_FORMAT_AMBIGUOUS; last-known-good retained.", key));
            return;
        }
        Path path = candidates.get(0);
        long started = System.nanoTime();
        queue.submit(VoiceDecodePriority.HOT_RELOAD, () -> decodeStable(path))
                .whenComplete((pcm, failure) -> Minecraft.getInstance().execute(() -> {
                    lastLatencyMillis = (System.nanoTime() - started) / 1_000_000L;
                    if (failure != null) {
                        retainKnownGood(key, failure);
                    } else {
                        commitIfCurrent(key, path, pcm);
                    }
                }));
    }

    public long lastLatencyMillis() {
        return lastLatencyMillis;
    }

    private DecodedPcm decodeStable(Path path) throws VoiceDecodeException {
        if (!stability.stable(path)) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_FILE_UNSTABLE,
                    "Voice file did not stabilize within bounded retries.");
        }
        VoiceAssetFormat format = VoiceAssetFormat.fromFilename(path.getFileName().toString()).orElseThrow();
        return decoders.require(format).decode(path, limits);
    }

    private void commitIfCurrent(VoiceAssetKey key, Path path, DecodedPcm pcm) {
        List<Path> current = resolver.supportedFiles(key);
        if (current.size() != 1 || !current.get(0).toAbsolutePath().normalize()
                .equals(path.toAbsolutePath().normalize())) {
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_HOT_RELOAD_CANDIDATE_REJECTED,
                    "Filesystem candidate changed during decode.", key));
            return;
        }
        try {
            VoiceAssetFormat format = VoiceAssetFormat.fromFilename(path.getFileName().toString()).orElseThrow();
            VoiceAssetEntry entry = catalog.commit(key, format, path, Files.size(path), pcm.durationMillis());
            cache.put(new VoiceCacheKey(key, entry.snapshot().revision()), pcm);
        } catch (IOException exception) {
            retainKnownGood(key, exception);
        }
    }

    private void retainKnownGood(VoiceAssetKey key, Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        VoiceDiagnosticCode code = cause instanceof VoiceDecodeException decode
                ? decode.code() : VoiceDiagnosticCode.VOICE_HOT_RELOAD_FAILED;
        diagnostics.accept(VoiceDiagnostic.warning(code,
                "Hot reload candidate rejected; last-known-good retained: " + cause.getMessage(), key));
        if (catalog.find(key).isPresent()) {
            diagnostics.accept(VoiceDiagnostic.warning(
                    VoiceDiagnosticCode.VOICE_ASSET_LAST_KNOWN_GOOD_RETAINED,
                    "Previous successfully committed revision remains active.", key));
        }
    }
}
