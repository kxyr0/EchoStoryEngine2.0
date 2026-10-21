package io.github.kxyr0.echoiteminspection.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class InspectionClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue WORLD_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue PROMPT_SCALE;
    public static final ForgeConfigSpec.IntValue PROMPT_MIN_WIDTH;
    public static final ForgeConfigSpec.IntValue PROMPT_HEIGHT;
    static {
        var builder = new ForgeConfigSpec.Builder();
        WORLD_DISTANCE = builder.comment("Maximum camera ray distance for IPLA world inspection, in blocks.")
                .defineInRange("worldInspectionDistance", 4.5, 1.0, 16.0);
        PROMPT_SCALE = builder.comment("Масштаб плашки Осмотреть вместе с текстом: 0.5–3.0.")
                .defineInRange("promptScale", 1.0, 0.5, 3.0);
        PROMPT_MIN_WIDTH = builder.comment("Минимальная ширина плашки при 1920x1080; 0 — по ширине текста.")
                .defineInRange("promptMinWidth", 0, 0, 1200);
        PROMPT_HEIGHT = builder.comment("Высота плашки при 1920x1080 до применения масштаба: 24–120.")
                .defineInRange("promptHeight", 36, 24, 120);
        SPEC = builder.build();
    }
    private InspectionClientConfig() { }
}
