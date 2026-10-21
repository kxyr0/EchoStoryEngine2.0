package io.github.kxyr0.echoanimation.forge.client.portrait;

import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client-side projection settings for drawing face-rig layers over dialogue visuals.
 */
public record PortraitOverlayConfig(
        double centerX,
        double centerY,
        double offsetX,
        double offsetY,
        double facePixels,
        Map<String, PortraitLayerConfig> layers,
        Map<String, PortraitBlinkConfig> blinks
) {
    private static final PortraitOverlayConfig DEFAULT =
            new PortraitOverlayConfig(0.5D, 0.5D, 0.0D, 0.0D, 8.0D, Map.of(), Map.of());

    public PortraitOverlayConfig {
        requireFinite(centerX, "centerX");
        requireFinite(centerY, "centerY");
        requireFinite(offsetX, "offsetX");
        requireFinite(offsetY, "offsetY");
        requireFinite(facePixels, "facePixels");
        if (facePixels <= 0.0D) {
            throw new IllegalArgumentException("facePixels должен быть положительным.");
        }
        layers = Map.copyOf(Objects.requireNonNull(layers, "layers"));
        blinks = Map.copyOf(Objects.requireNonNull(blinks, "blinks"));
    }

    public static PortraitOverlayConfig defaultConfig() {
        return DEFAULT;
    }

    public PortraitLayerConfig layer(String id) {
        return layers.getOrDefault(id, PortraitLayerConfig.identity());
    }

    public List<FaceControlDefinition> controls(List<FaceControlDefinition> controls) {
        return controls.stream()
                .map(control -> blinks.getOrDefault(control.id().value(), PortraitBlinkConfig.identity())
                        .apply(control))
                .toList();
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " должен быть finite.");
        }
    }
}
