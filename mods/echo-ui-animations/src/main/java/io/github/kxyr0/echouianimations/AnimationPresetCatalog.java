package io.github.kxyr0.echouianimations;

import java.util.List;

/**
 * Built-in UI animation presets.
 */
public final class AnimationPresetCatalog {
    public static final AnimationPreset FADE_IN = preset(
            "fade_in", 140, AnimationEasing.EASE_OUT_CUBIC, channel(0.0F, 1.0F));
    public static final AnimationPreset FADE_OUT = preset(
            "fade_out", 110, AnimationEasing.EASE_OUT_CUBIC, channel(1.0F, 0.0F));
    public static final AnimationPreset PANEL_OPEN = new AnimationPreset(
            "panel_open", 170, 0, AnimationEasing.EASE_OUT_BACK,
            channel(0.0F, 1.0F), channel(0.94F, 1.0F),
            channel(0.96F, 1.0F), channel(0.80F, 1.0F), false);
    public static final AnimationPreset PANEL_CLOSE = new AnimationPreset(
            "panel_close", 120, 0, AnimationEasing.EASE_OUT_CUBIC,
            channel(1.0F, 0.0F), channel(1.0F, 0.98F),
            channel(1.0F, 0.98F), channel(1.0F, 0.86F), false);
    public static final AnimationPreset PANEL_RESIZE = new AnimationPreset(
            "panel_resize", 150, 0, AnimationEasing.EASE_IN_OUT_CUBIC,
            AnimationChannel.fixed(1.0F), AnimationChannel.fixed(1.0F),
            channel(0.0F, 1.0F), channel(0.0F, 1.0F), false);
    public static final AnimationPreset TYPEWRITER = new AnimationPreset(
            "typewriter", 360, 0, AnimationEasing.LINEAR,
            AnimationChannel.fixed(1.0F), AnimationChannel.fixed(1.0F),
            AnimationChannel.fixed(1.0F), AnimationChannel.fixed(1.0F), true);
    public static final AnimationPreset CHOICE_HOVER = new AnimationPreset(
            "choice_hover", 90, 0, AnimationEasing.EASE_OUT_CUBIC,
            AnimationChannel.fixed(1.0F), channel(1.0F, 1.025F),
            AnimationChannel.fixed(1.0F), AnimationChannel.fixed(1.0F), false);

    private static final List<AnimationPreset> ALL = List.of(
            FADE_IN, FADE_OUT, PANEL_OPEN, PANEL_CLOSE, PANEL_RESIZE, TYPEWRITER, CHOICE_HOVER);

    private AnimationPresetCatalog() {
    }

    public static List<AnimationPreset> all() {
        return ALL;
    }

    private static AnimationPreset preset(
            String id,
            int durationMs,
            AnimationEasing easing,
            AnimationChannel alpha
    ) {
        return new AnimationPreset(id, durationMs, 0, easing, alpha, AnimationChannel.fixed(1.0F),
                AnimationChannel.fixed(1.0F), AnimationChannel.fixed(1.0F), false);
    }

    private static AnimationChannel channel(float from, float to) {
        return new AnimationChannel(from, to);
    }
}
