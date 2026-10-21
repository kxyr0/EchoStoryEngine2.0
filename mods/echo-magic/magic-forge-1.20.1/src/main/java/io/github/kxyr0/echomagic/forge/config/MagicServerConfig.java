package io.github.kxyr0.echomagic.forge.config;

import io.github.kxyr0.echomagic.core.validation.SpellSafetyLimits;
import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue KNOWN;
    private static final ForgeConfigSpec.IntValue CASTS;
    private static final ForgeConfigSpec.IntValue PROJECTILES;
    private static final ForgeConfigSpec.DoubleValue RANGE;
    private static final ForgeConfigSpec.DoubleValue SPEED;
    private static final ForgeConfigSpec.IntValue BURST;
    private static final ForgeConfigSpec.DoubleValue DAMAGE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_GESTURE_SLOWDOWN_ON_DEDICATED_SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        KNOWN = builder.defineInRange("maximum_known_spells_per_player", 128, 1, 4096);
        CASTS = builder.defineInRange("maximum_active_casts_per_player", 8, 1, 128);
        PROJECTILES = builder.defineInRange("maximum_active_projectiles_per_player", 64, 1, 1024);
        RANGE = builder.defineInRange("maximum_projectile_range", 128.0D, 1.0D, 4096.0D);
        SPEED = builder.defineInRange("maximum_projectile_speed", 8.0D, 0.01D, 64.0D);
        BURST = builder.defineInRange("maximum_burst_count", 32, 1, 512);
        DAMAGE = builder.defineInRange("maximum_damage_per_hit", 100.0D, 0.01D, 10000.0D);
        ALLOW_GESTURE_SLOWDOWN_ON_DEDICATED_SERVER = builder
                .comment("Allow clients to slow a dedicated server while drawing gestures.")
                .define("allow_gesture_slowdown_on_dedicated_server", false);
        SPEC = builder.build();
    }

    public static SpellSafetyLimits limits() {
        return new SpellSafetyLimits(KNOWN.get(), CASTS.get(), PROJECTILES.get(), RANGE.get(), SPEED.get(),
                BURST.get(), DAMAGE.get());
    }
    private MagicServerConfig() { }
}
