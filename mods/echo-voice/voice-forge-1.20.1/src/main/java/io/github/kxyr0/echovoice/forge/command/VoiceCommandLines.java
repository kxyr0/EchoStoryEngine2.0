package io.github.kxyr0.echovoice.forge.command;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackSnapshot;
import io.github.kxyr0.echovoice.forge.api.VoiceQueryService;
import io.github.kxyr0.echovoice.forge.client.VoiceClientRuntime;
import java.util.ArrayList;
import java.util.List;

/** Stable string rendering that never exposes Java object identities. */
final class VoiceCommandLines {
    private VoiceCommandLines() {
    }

    static List<String> status(VoiceClientRuntime runtime) {
        VoicePlaybackSnapshot state = runtime.query().playback().snapshot();
        var decode = runtime.decodeMetrics();
        List<String> lines = new ArrayList<>();
        lines.add("status=" + state.status() + " owner="
                + state.request().map(request -> request.owner().name()).orElse("NONE"));
        state.request().ifPresent(request -> lines.add("execution=" + request.dialogueExecutionId()
                + " presentation_revision=" + request.linePresentationRevision()
                + " generation=" + request.presentationGeneration()));
        lines.add("playback_id=" + state.playbackId().map(Object::toString).orElse("NONE")
                + " played_ms=" + state.playedMillis() + " active_sources=" + state.activeSourceCount()
                + " source_state=" + (state.activeSourceCount() == 0 ? "INACTIVE" : "ACTIVE"));
        state.asset().ifPresent(asset -> lines.add(formatAsset(asset)));
        lines.add("decoder=" + decode.decoder() + " decode_latency_ms=" + decode.latencyMillis()
                + " cache_result=" + decode.cacheResult());
        lines.add("engine_generation=" + state.soundEngineGeneration() + " stop_reason="
                + state.stopReason().map(Enum::name).orElse("NONE")
                + " stale_rejects=" + state.staleStartRejects());
        lines.addAll(cache(runtime));
        lines.add("catalog_scan_ms=" + runtime.catalogScanMillis() + " hot_reload_ms="
                + runtime.hotReloadLatencyMillis() + " preload_hit_ratio=" + ratio(runtime.preloadHitRatio())
                + " preload_queue=" + runtime.decodeQueueSize());
        lines.add("diagnostics=" + runtime.query().diagnostics().recent().size());
        return lines;
    }

    static List<String> assets(VoiceQueryService query) {
        List<String> lines = new ArrayList<>();
        lines.add("assets=" + query.assets().assets().size());
        query.assets().assets().forEach(asset -> lines.add(formatAsset(asset)));
        return lines;
    }

    static List<String> asset(VoiceQueryService query, VoiceAssetKey key) {
        return query.assets().find(key).map(asset -> List.of(formatAsset(asset)))
                .orElseGet(() -> List.of("VOICE_ASSET_NOT_FOUND " + key));
    }

    static List<String> cache(VoiceClientRuntime runtime) {
        return List.of("cache_entries=" + runtime.cacheEntries() + " cache_bytes=" + runtime.cacheBytes()
                + " cache_mb=" + ratio(runtime.cacheBytes() / 1024.0 / 1024.0)
                + " hit_ratio=" + ratio(runtime.cacheHitRatio()),
                "decode_queue=" + runtime.decodeQueueSize());
    }

    static List<String> diagnostics(VoiceQueryService query) {
        List<String> lines = new ArrayList<>();
        lines.add("diagnostics=" + query.diagnostics().recent().size());
        query.diagnostics().recent().forEach(diagnostic -> lines.add(diagnostic.code() + " "
                + diagnostic.severity() + " asset=" + diagnostic.assetKey().map(Object::toString).orElse("NONE")
                + " message=" + diagnostic.message()));
        return lines;
    }

    private static String formatAsset(VoiceAssetSnapshot asset) {
        return "asset=" + asset.key() + " stem=" + asset.key().stem() + " format=" + asset.format()
                + " revision=" + asset.revision() + " bytes=" + asset.byteSize()
                + " duration_ms=" + asset.durationMillis() + " source=" + asset.sourcePath();
    }

    private static String ratio(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
