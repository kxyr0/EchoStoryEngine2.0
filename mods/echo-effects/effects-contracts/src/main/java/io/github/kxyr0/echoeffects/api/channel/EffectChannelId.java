package io.github.kxyr0.echoeffects.api.channel;

import java.util.Objects;

public record EffectChannelId(String value) {
    public static final EffectChannelId MOVEMENT_SPEED = new EffectChannelId("movement.speed_multiplier");
    public static final EffectChannelId CAMERA_FOV = new EffectChannelId("camera.fov_offset");
    public static final EffectChannelId CAMERA_ROLL = new EffectChannelId("camera.roll");
    public static final EffectChannelId CAMERA_SHAKE = new EffectChannelId("camera.shake");
    public static final EffectChannelId SCREEN_TINT = new EffectChannelId("screen.tint");
    public static final EffectChannelId SCREEN_VIGNETTE = new EffectChannelId("screen.vignette");
    public static final EffectChannelId SOUND_PLAY = new EffectChannelId("sound.play_at");
    public static final EffectChannelId VANILLA_MOB_EFFECT_APPLY =
            new EffectChannelId("vanilla.mob_effect.apply");

    public EffectChannelId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EffectChannelId must not be blank.");
        }
    }
}
