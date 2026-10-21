package io.github.kxyr0.echoplayer.forge.api.appearance.resolved;

import java.util.Objects;

/**
 * Public render descriptor for a standard Minecraft baked wearable model.
 */
public record WearableModelDescriptor(
        String wearableId,
        String modelId,
        double x,
        double y,
        double z,
        double pitch,
        double yaw,
        double roll,
        double scaleX,
        double scaleY,
        double scaleZ
) {
    public WearableModelDescriptor {
        wearableId = Objects.requireNonNull(wearableId, "wearableId");
        modelId = Objects.requireNonNull(modelId, "modelId");
    }
}
