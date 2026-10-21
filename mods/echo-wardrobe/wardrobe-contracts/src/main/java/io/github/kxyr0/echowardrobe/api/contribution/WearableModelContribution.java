package io.github.kxyr0.echowardrobe.api.contribution;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;
import java.util.Objects;

/**
 * Standard Minecraft/Blockbench item model contribution.
 */
public record WearableModelContribution(String modelResourceId, WearableModelTransform transform) {
    public WearableModelContribution {
        modelResourceId = WardrobeResourceId.require(modelResourceId, "Model resource ID");
        transform = Objects.requireNonNull(transform, "transform");
    }
}
