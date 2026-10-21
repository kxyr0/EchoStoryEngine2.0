package io.github.kxyr0.echovoice.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.api.VoiceQueryService;
import io.github.kxyr0.echovoice.forge.api.VoiceService;
import io.github.kxyr0.echovoice.forge.cache.VoiceDecodedCache;
import io.github.kxyr0.echovoice.forge.catalog.FilesystemVoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetPathResolver;
import io.github.kxyr0.echovoice.forge.catalog.VoiceCatalogScanner;
import io.github.kxyr0.echovoice.forge.config.EchoVoiceClientConfig;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.codec.VoiceDecoderRegistry;
import io.github.kxyr0.echovoice.forge.decode.queue.VoiceDecodeQueue;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceDiagnosticBuffer;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import io.github.kxyr0.echovoice.forge.playback.lane.VoiceDecodeMetrics;
import io.github.kxyr0.echovoice.forge.playback.lane.VoiceForegroundLane;
import io.github.kxyr0.echovoice.forge.playback.ambient.ForgeAmbientVoiceService;
import io.github.kxyr0.echovoice.forge.playback.sound.VoiceStopRamp;
import io.github.kxyr0.echovoice.forge.reload.VoiceHotReloadPipeline;
import io.github.kxyr0.echovoice.forge.reload.stability.VoiceFileStabilityProbe;
import io.github.kxyr0.echovoice.forge.reload.watch.VoiceFileWatcher;
import io.github.kxyr0.echovoice.forge.reload.watch.VoiceReloadDebouncer;
import io.github.kxyr0.echovoice.forge.service.ForgeVoiceQueryService;
import io.github.kxyr0.echovoice.forge.service.ForgeVoiceService;
import io.github.kxyr0.echovoice.forge.service.VoicePlaybackListeners;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Client composition root and lifecycle owner. */
public final class VoiceClientRuntime implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final FilesystemVoiceAssetCatalog catalog;
    private final VoiceDecodedCache cache;
    private final VoiceDecodeQueue queue;
    private final VoiceForegroundLane lane;
    private final ForgeVoiceService service;
    private final ForgeVoiceQueryService query;
    private final ForgeAmbientVoiceService ambient;
    private final VoiceCatalogScanner scanner;
    private final VoiceReloadDebouncer debouncer;
    private final VoiceHotReloadPipeline reload;
    private final VoiceFileWatcher watcher;
    private final VoiceStopRamp stopRamp;
    private final long catalogScanMillis;

    private VoiceClientRuntime(FilesystemVoiceAssetCatalog catalog, VoiceDecodedCache cache, VoiceDecodeQueue queue,
            VoiceForegroundLane lane, ForgeVoiceService service, ForgeVoiceQueryService query,
            VoiceCatalogScanner scanner, VoiceReloadDebouncer debouncer, VoiceHotReloadPipeline reload,
            VoiceFileWatcher watcher, VoiceStopRamp stopRamp, ForgeAmbientVoiceService ambient,
            long catalogScanMillis) {
        this.catalog = catalog;
        this.cache = cache;
        this.queue = queue;
        this.lane = lane;
        this.service = service;
        this.query = query;
        this.ambient = ambient;
        this.scanner = scanner;
        this.debouncer = debouncer;
        this.reload = reload;
        this.watcher = watcher;
        this.stopRamp = stopRamp;
        this.catalogScanMillis = catalogScanMillis;
    }

    public static VoiceClientRuntime create(Minecraft minecraft) throws IOException {
        VoiceDiagnosticBuffer buffer = new VoiceDiagnosticBuffer(200);
        VoiceLogSink diagnostics = new VoiceLogSink(buffer);
        VoiceAssetPathResolver resolver = new VoiceAssetPathResolver(minecraft.gameDirectory.toPath());
        FilesystemVoiceAssetCatalog catalog = new FilesystemVoiceAssetCatalog();
        VoiceDecodedCache cache = new VoiceDecodedCache(megabytes(EchoVoiceClientConfig.DECODED_CACHE_MB.get()));
        VoiceDecodeQueue queue = new VoiceDecodeQueue(EchoVoiceClientConfig.DECODE_WORKERS.get(), 32);
        VoiceDecoderRegistry decoders = new VoiceDecoderRegistry();
        VoiceDecodeLimits limits = new VoiceDecodeLimits(megabytes(EchoVoiceClientConfig.MAXIMUM_FILE_MB.get()),
                EchoVoiceClientConfig.MAXIMUM_DURATION_SECONDS.get() * 1000L);
        VoicePlaybackListeners listeners = new VoicePlaybackListeners();
        VoiceStopRamp stopRamp = new VoiceStopRamp();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(stopRamp::started);
        VoiceForegroundLane lane = new VoiceForegroundLane(listeners, diagnostics, stopRamp);
        ForgeVoiceService service = new ForgeVoiceService(catalog, cache, queue, decoders, limits, lane, listeners,
                diagnostics);
        ForgeAmbientVoiceService ambient = new ForgeAmbientVoiceService(catalog, cache, queue, decoders, limits,
                lane);
        ForgeVoiceQueryService query = new ForgeVoiceQueryService(catalog, lane, buffer);
        VoiceCatalogScanner scanner = new VoiceCatalogScanner(resolver, catalog, diagnostics);
        VoiceReloadDebouncer debouncer = new VoiceReloadDebouncer();
        VoiceFileStabilityProbe stability = new VoiceFileStabilityProbe(
                EchoVoiceClientConfig.HOT_RELOAD_STABILITY_CHECKS.get(),
                EchoVoiceClientConfig.HOT_RELOAD_RETRY_COUNT.get(), 150);
        VoiceHotReloadPipeline reload = new VoiceHotReloadPipeline(resolver, catalog, cache, queue, decoders, limits,
                stability, diagnostics);
        VoiceFileWatcher watcher = new VoiceFileWatcher(resolver, debouncer, diagnostics);
        long scanMillis = scanner.scan();
        VoiceClientRuntime runtime = new VoiceClientRuntime(catalog, cache, queue, lane, service, query, scanner,
                debouncer, reload, watcher, stopRamp, ambient, scanMillis);
        LOGGER.info("Echo Voice asset catalog ready: assets={}, scan_ms={}", catalog.assets().size(), scanMillis);
        return runtime;
    }

    public void tick(Minecraft minecraft) {
        debouncer.due(System.currentTimeMillis(), EchoVoiceClientConfig.HOT_RELOAD_DEBOUNCE_MS.get())
                .forEach(reload::reload);
        lane.tick(minecraft);
        ambient.tick(minecraft);
    }

    public VoiceService service() {
        return service;
    }

    public VoiceQueryService query() {
        return query;
    }

    public ForgeAmbientVoiceService ambient() { return ambient; }

    public void stop(VoiceStopReason reason) {
        service.stop(reason);
    }

    public void soundEngineReloaded() {
        stopRamp.reset();
        lane.soundEngineReloaded();
    }

    public void reloadAll() {
        java.util.Set<io.github.kxyr0.echovoice.api.identity.VoiceAssetKey> keys = new java.util.LinkedHashSet<>();
        catalog.assets().forEach(asset -> keys.add(asset.key()));
        keys.addAll(scanner.discoverKeys());
        keys.forEach(reload::reload);
    }

    public long cacheBytes() {
        return cache.bytes();
    }

    public int cacheEntries() {
        return cache.entryCount();
    }

    public double cacheHitRatio() {
        return cache.hitRatio();
    }

    public int decodeQueueSize() {
        return queue.queuedTasks();
    }

    public long hotReloadLatencyMillis() {
        return reload.lastLatencyMillis();
    }

    public long catalogScanMillis() {
        return catalogScanMillis;
    }

    public double preloadHitRatio() {
        return service.preloadHitRatio();
    }

    public VoiceDecodeMetrics decodeMetrics() {
        return lane.decodeMetrics();
    }

    @Override
    public void close() throws IOException {
        service.stop(VoiceStopReason.EXPLICIT_STOP);
        ambient.stopAll();
        watcher.close();
        queue.close();
        stopRamp.close();
    }

    private static long megabytes(int value) {
        return value * 1024L * 1024L;
    }
}
