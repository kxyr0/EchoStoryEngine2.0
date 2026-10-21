package io.github.kxyr0.echodialogue.forge.client.visual;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualId;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolved client texture metadata for one speaker visual.
 */
public record ClientSpeakerVisual(
        SpeakerVisualKind kind,
        SpeakerVisualId id,
        ResourceLocation texture,
        int sourceWidth,
        int sourceHeight,
        boolean skinSheet
) {
    public ClientSpeakerVisual {
        kind = Objects.requireNonNull(kind, "kind");
        id = Objects.requireNonNull(id, "id");
        texture = Objects.requireNonNull(texture, "texture");
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("Visual texture dimensions must be positive.");
        }
    }

    public static ClientSpeakerVisual image(
            SpeakerVisualKind kind,
            SpeakerVisualId id,
            ResourceLocation texture,
            int sourceWidth,
            int sourceHeight
    ) {
        return new ClientSpeakerVisual(kind, id, texture, sourceWidth, sourceHeight, false);
    }

    public static ClientSpeakerVisual skinSheet(
            SpeakerVisualKind kind,
            SpeakerVisualId id,
            ResourceLocation texture,
            int sourceWidth,
            int sourceHeight
    ) {
        return new ClientSpeakerVisual(kind, id, texture, sourceWidth, sourceHeight, true);
    }
}
