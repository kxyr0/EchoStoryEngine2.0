package io.github.kxyr0.echothoughtspace.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ThoughtSpaceClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue FADE_MILLIS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        FADE_MILLIS = builder.comment("Opening and closing fade duration in milliseconds.")
                .defineInRange("fadeMillis", 260, 220, 300);
        SPEC = builder.build();
    }

    private ThoughtSpaceClientConfig() { }
    public static int fadeMillis() { return FADE_MILLIS.get(); }
}

