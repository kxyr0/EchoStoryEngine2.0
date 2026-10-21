package io.github.kxyr0.echonpc.forge.api.client.render;

import java.util.Objects;

/**
 * Immutable metadata for one public NPC client render extension.
 */
public record NpcClientRenderExtensionDescriptor(NpcClientRenderExtensionId id, String displayName) {
    public NpcClientRenderExtensionDescriptor {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must be non-empty.");
        }
    }
}
