package io.github.kxyr0.echonpcinteraction.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class InteractionServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue MAXIMUM_INTERACTION_DISTANCE;
    public static final ForgeConfigSpec.IntValue APPROACH_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue ALIGNMENT_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue ALIGNMENT_TOLERANCE_DEGREES;
    public static final ForgeConfigSpec.IntValue INTERACTION_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.IntValue VERIFICATION_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.IntValue MAXIMUM_REPLANS;
    public static final ForgeConfigSpec.BooleanValue PROXY_WORLD_EFFECTS_ONLY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAXIMUM_INTERACTION_DISTANCE = builder.defineInRange("maximum_interaction_distance", 5.0D, 1.0D, 16.0D);
        APPROACH_SEARCH_RADIUS = builder.defineInRange("approach_search_radius", 3, 1, 8);
        ALIGNMENT_DURATION_TICKS = builder.defineInRange("alignment_duration_ticks", 6, 1, 40);
        ALIGNMENT_TOLERANCE_DEGREES = builder.defineInRange("alignment_tolerance_degrees", 5.0D, 0.5D, 45.0D);
        INTERACTION_TIMEOUT_TICKS = builder.defineInRange("interaction_timeout_ticks", 200, 20, 2400);
        VERIFICATION_TIMEOUT_TICKS = builder.defineInRange("verification_timeout_ticks", 40, 1, 400);
        MAXIMUM_REPLANS = builder.defineInRange("maximum_replans", 2, 0, 8);
        PROXY_WORLD_EFFECTS_ONLY = builder.define("proxy_world_effects_only", true);
        SPEC = builder.build();
    }

    private InteractionServerConfig() {
    }
}
