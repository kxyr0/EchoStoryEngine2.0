package io.github.kxyr0.echostoryengine.story.extension.forge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * Forge-only context passed when a server-scoped extension session is created.
 */
public record ForgeStoryExtensionContext(
        MinecraftServer server,
        StoryExtensionApiVersion engineApiVersion) {
    public ForgeStoryExtensionContext {
        server = Objects.requireNonNull(server, "server");
        engineApiVersion = Objects.requireNonNull(engineApiVersion, "engineApiVersion");
    }
}
