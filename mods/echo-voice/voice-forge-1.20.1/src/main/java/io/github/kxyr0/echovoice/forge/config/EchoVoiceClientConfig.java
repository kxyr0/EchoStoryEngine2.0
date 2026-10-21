package io.github.kxyr0.echovoice.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Bounded client-only safety and latency configuration. */
public final class EchoVoiceClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAXIMUM_FILE_MB;
    public static final ForgeConfigSpec.IntValue MAXIMUM_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue HOT_RELOAD_DEBOUNCE_MS;
    public static final ForgeConfigSpec.IntValue HOT_RELOAD_STABILITY_CHECKS;
    public static final ForgeConfigSpec.IntValue HOT_RELOAD_RETRY_COUNT;
    public static final ForgeConfigSpec.IntValue DECODED_CACHE_MB;
    public static final ForgeConfigSpec.IntValue PRELOAD_AHEAD_LINES;
    public static final ForgeConfigSpec.IntValue DECODE_WORKERS;
    public static final ForgeConfigSpec.IntValue STOP_RAMP_MS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("voice");
        MAXIMUM_FILE_MB = builder.defineInRange("maximum_voice_file_mb", 64, 1, 512);
        MAXIMUM_DURATION_SECONDS = builder.defineInRange("maximum_voice_duration_seconds", 600, 1, 3600);
        HOT_RELOAD_DEBOUNCE_MS = builder.defineInRange("hot_reload_debounce_ms", 400, 100, 5000);
        HOT_RELOAD_STABILITY_CHECKS = builder.defineInRange("hot_reload_stability_checks", 2, 1, 10);
        HOT_RELOAD_RETRY_COUNT = builder.defineInRange("hot_reload_retry_count", 4, 1, 20);
        DECODED_CACHE_MB = builder.defineInRange("decoded_cache_mb", 256, 16, 2048);
        PRELOAD_AHEAD_LINES = builder.defineInRange("preload_ahead_lines", 2, 0, 8);
        DECODE_WORKERS = builder.defineInRange("decode_workers", 2, 1, 4);
        STOP_RAMP_MS = builder.defineInRange("stop_ramp_ms", 8, 0, 20);
        builder.pop();
        SPEC = builder.build();
    }

    private EchoVoiceClientConfig() {
    }
}
