package io.github.kxyr0.echowardrobe.api.contribution;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;
import java.util.List;
import java.util.Objects;

/**
 * 2D dialogue portrait overlay authored for a wearable.
 */
public record WearablePortraitOverlayContribution(String textureResourceId, List<WearablePortraitStrip> strips) {
    public WearablePortraitOverlayContribution {
        textureResourceId = WardrobeResourceId.require(textureResourceId, "Portrait texture resource ID");
        Objects.requireNonNull(strips, "strips");
        if (strips.isEmpty()) {
            throw new IllegalArgumentException("Portrait overlay requires at least one strip.");
        }
        strips = List.copyOf(strips);
    }
}
