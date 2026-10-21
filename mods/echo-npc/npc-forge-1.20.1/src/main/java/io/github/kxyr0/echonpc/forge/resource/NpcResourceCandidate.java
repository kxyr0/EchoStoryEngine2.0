package io.github.kxyr0.echonpc.forge.resource;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record NpcResourceCandidate(ResourceLocation location, String sourceText) {
    public NpcResourceCandidate {
        location = Objects.requireNonNull(location, "location");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
    }

    public String sourceId() {
        return location.toString();
    }
}
