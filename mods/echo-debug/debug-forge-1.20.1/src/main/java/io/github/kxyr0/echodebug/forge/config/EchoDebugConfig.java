package io.github.kxyr0.echodebug.forge.config;

import io.github.kxyr0.echodebug.core.trail.NpcTrailConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class EchoDebugConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final ForgeConfigSpec.DoubleValue RENDER_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_NPC_MOVEMENT_TRAILS;
    private static final ForgeConfigSpec.DoubleValue MINIMUM_TRAIL_SAMPLE_DISTANCE;
    private static final ForgeConfigSpec.IntValue MAXIMUM_TRAIL_SAMPLE_INTERVAL_TICKS;
    private static final ForgeConfigSpec.IntValue MAX_POINTS_PER_TRAIL;
    private static final ForgeConfigSpec.IntValue MAX_TRAILS;
    private static final ForgeConfigSpec.DoubleValue TRAIL_LINE_WIDTH;
    private static final ForgeConfigSpec.DoubleValue TERMINAL_TRAIL_OPACITY;
    private static final ForgeConfigSpec.DoubleValue ACTIVE_TRAIL_OPACITY;
    private static final ForgeConfigSpec.DoubleValue NPC_TRAIL_RENDER_DISTANCE;

    static {
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        RENDER_DISTANCE = client.comment("Trigger overlay render distance in blocks.")
                .defineInRange("renderDistance", 512.0D, 16.0D, 4096.0D);
        SHOW_NPC_MOVEMENT_TRAILS = client.comment("Draw actual NPC movement trails captured from Echo Navigation.")
                .define("show_npc_movement_trails", true);
        MINIMUM_TRAIL_SAMPLE_DISTANCE = client.comment("Minimum NPC movement in blocks before adding a trail point.")
                .defineInRange("minimum_trail_sample_distance", 0.15D, 0.0D, 64.0D);
        MAXIMUM_TRAIL_SAMPLE_INTERVAL_TICKS = client.comment("Maximum server ticks between trail points.")
                .defineInRange("maximum_trail_sample_interval_ticks", 10, 1, 1200);
        MAX_POINTS_PER_TRAIL = client.comment("Maximum stored points per NPC movement trail.")
                .defineInRange("max_points_per_trail", 4096, 2, 65536);
        MAX_TRAILS = client.comment("Maximum stored NPC movement trails.")
                .defineInRange("max_trails", 64, 1, 1024);
        TRAIL_LINE_WIDTH = client.comment("NPC movement trail line width.")
                .defineInRange("trail_line_width", 2.0D, 1.0D, 16.0D);
        TERMINAL_TRAIL_OPACITY = client.comment("Opacity for completed NPC movement trails.")
                .defineInRange("terminal_trail_opacity", 0.45D, 0.0D, 1.0D);
        ACTIVE_TRAIL_OPACITY = client.comment("Opacity for active NPC movement trails.")
                .defineInRange("active_trail_opacity", 0.9D, 0.0D, 1.0D);
        NPC_TRAIL_RENDER_DISTANCE = client.comment("NPC movement trail render distance in blocks.")
                .defineInRange("npc_trail_render_distance", 256.0D, 16.0D, 4096.0D);
        CLIENT_SPEC = client.build();
    }

    private EchoDebugConfig() {
    }

    public static double renderDistance() {
        return RENDER_DISTANCE.get();
    }

    public static boolean showNpcMovementTrails() {
        return SHOW_NPC_MOVEMENT_TRAILS.get();
    }

    public static NpcTrailConfig npcTrailConfig() {
        return new NpcTrailConfig(MINIMUM_TRAIL_SAMPLE_DISTANCE.get(), MAXIMUM_TRAIL_SAMPLE_INTERVAL_TICKS.get(),
                MAX_POINTS_PER_TRAIL.get(), MAX_TRAILS.get());
    }

    public static float trailLineWidth() {
        return TRAIL_LINE_WIDTH.get().floatValue();
    }

    public static float trailOpacity(boolean active) {
        return (active ? ACTIVE_TRAIL_OPACITY.get() : TERMINAL_TRAIL_OPACITY.get()).floatValue();
    }

    public static double npcTrailRenderDistance() {
        return NPC_TRAIL_RENDER_DISTANCE.get();
    }
}
