package io.github.kxyr0.echoanimation.forge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public explicit Forge bridge for resolving the server-scoped AnimationService.
 */
public final class AnimationServiceBridge {
    private AnimationServiceBridge() {
    }

    public static AnimationServiceLookupResult lookup(MinecraftServer server) {
        AnimationServiceLookupEvent event = new AnimationServiceLookupEvent(
                Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new AnimationServiceLookupResult(AnimationServiceLookupResult.Status.UNAVAILABLE,
                    Optional.empty(), "Echo Animation service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new AnimationServiceLookupResult(AnimationServiceLookupResult.Status.DUPLICATE_PROVIDER,
                    Optional.empty(), "More than one Echo Animation service provider responded.");
        }
        return new AnimationServiceLookupResult(AnimationServiceLookupResult.Status.RESOLVED,
                Optional.of(event.providers().get(0)), "Echo Animation service resolved.");
    }
}
