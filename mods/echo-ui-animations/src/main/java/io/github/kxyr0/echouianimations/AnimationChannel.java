package io.github.kxyr0.echouianimations;

/**
 * One scalar animation channel.
 */
public record AnimationChannel(float from, float to) {
    public static AnimationChannel fixed(float value) {
        return new AnimationChannel(value, value);
    }

    public float sample(float progress) {
        return from + (to - from) * progress;
    }
}
