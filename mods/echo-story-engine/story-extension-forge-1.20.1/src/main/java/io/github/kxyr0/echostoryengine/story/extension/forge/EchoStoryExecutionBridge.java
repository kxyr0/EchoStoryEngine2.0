package io.github.kxyr0.echostoryengine.story.extension.forge;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public Forge bridge for resolving the server-scoped Story execution starter.
 */
public final class EchoStoryExecutionBridge {
    private EchoStoryExecutionBridge() {
    }

    public static EchoStoryExecutionLookupResult lookup(MinecraftServer server) {
        EchoStoryExecutionLookupEvent event = new EchoStoryExecutionLookupEvent(
                Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new EchoStoryExecutionLookupResult(EchoStoryExecutionLookupResult.Status.UNAVAILABLE,
                    java.util.Optional.empty(), "Echo Story execution starter is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new EchoStoryExecutionLookupResult(EchoStoryExecutionLookupResult.Status.DUPLICATE_PROVIDER,
                    java.util.Optional.empty(), "More than one Echo Story execution starter responded.");
        }
        return new EchoStoryExecutionLookupResult(EchoStoryExecutionLookupResult.Status.RESOLVED,
                java.util.Optional.of(event.providers().get(0)), "Echo Story execution starter resolved.");
    }
}
