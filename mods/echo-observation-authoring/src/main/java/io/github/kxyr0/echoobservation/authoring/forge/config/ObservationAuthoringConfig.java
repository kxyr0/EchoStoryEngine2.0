package io.github.kxyr0.echoobservation.authoring.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ObservationAuthoringConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Hard-enable local Observation source authoring tools.")
                .define("enabled", true);
        SPEC = builder.build();
    }

    private ObservationAuthoringConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }
}
