package io.github.kxyr0.echouianimations;

/**
 * Sampled values for a single render tick.
 */
public record AnimationFrame(
        float progress,
        float alpha,
        float scale,
        float widthFactor,
        float heightFactor,
        int visibleCharacters,
        boolean finished
) {
}
