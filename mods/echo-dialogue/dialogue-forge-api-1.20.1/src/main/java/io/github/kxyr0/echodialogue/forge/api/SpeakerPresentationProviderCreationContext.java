package io.github.kxyr0.echodialogue.forge.api;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * Forge-only server context passed to provider factories during host startup.
 */
public record SpeakerPresentationProviderCreationContext(MinecraftServer server) {
    public SpeakerPresentationProviderCreationContext {
        server = Objects.requireNonNull(server, "server");
    }
}
