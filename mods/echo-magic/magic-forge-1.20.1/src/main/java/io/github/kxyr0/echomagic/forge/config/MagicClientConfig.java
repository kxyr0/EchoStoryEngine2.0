package io.github.kxyr0.echomagic.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAXIMUM_PARTICLES_PER_FRAME;
    public static final ForgeConfigSpec.IntValue MAXIMUM_PARTICLES_ACTIVE;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_QUALITY;
    public static final ForgeConfigSpec.BooleanValue USE_VANILLA_COLORED_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue ECHO_PARTICLE_SCALE;
    public static final ForgeConfigSpec.DoubleValue VANILLA_PARTICLE_SCALE;
    public static final ForgeConfigSpec.BooleanValue SHOW_REJECTION;
    public static final ForgeConfigSpec.BooleanValue SLOW_TIME_WHILE_DRAWING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAXIMUM_PARTICLES_PER_FRAME = builder.defineInRange("maximum_magic_particles_per_frame", 512, 16, 8192);
        MAXIMUM_PARTICLES_ACTIVE = builder.defineInRange("maximum_magic_particles_active", 4096, 64, 32768);
        PARTICLE_QUALITY = builder.define("particle_quality", "HIGH");
        USE_VANILLA_COLORED_PARTICLES = builder.comment(
                "Replace spell-defined particles with vanilla dust while preserving each DSL color.")
                .define("use_vanilla_colored_particles", false);
        ECHO_PARTICLE_SCALE = builder.comment("Visual scale multiplier for Echo billboard particles.")
                .defineInRange("echo_particle_scale", 0.70D, 0.25D, 2.0D);
        VANILLA_PARTICLE_SCALE = builder.comment("Visual scale multiplier for vanilla colored dust.")
                .defineInRange("vanilla_particle_scale", 3.0D, 0.5D, 8.0D);
        SHOW_REJECTION = builder.define("show_cast_rejection_message", true);
        SLOW_TIME_WHILE_DRAWING = builder.comment(
                "Slow all non-player living entities while a magic gesture is drawn.")
                .define("slow_time_while_drawing", true);
        SPEC = builder.build();
    }
    private MagicClientConfig() { }
}
