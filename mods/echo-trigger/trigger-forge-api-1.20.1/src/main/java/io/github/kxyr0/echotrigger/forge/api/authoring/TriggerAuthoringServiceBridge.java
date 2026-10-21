package io.github.kxyr0.echotrigger.forge.api.authoring;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class TriggerAuthoringServiceBridge {
    private TriggerAuthoringServiceBridge() {
    }

    public static TriggerAuthoringServiceLookupResult lookup(MinecraftServer server) {
        TriggerAuthoringServiceLookupEvent event = new TriggerAuthoringServiceLookupEvent(
                Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new TriggerAuthoringServiceLookupResult(
                    TriggerAuthoringServiceLookupResult.Status.UNAVAILABLE,
                    Optional.empty(),
                    "Echo Trigger authoring service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new TriggerAuthoringServiceLookupResult(
                    TriggerAuthoringServiceLookupResult.Status.DUPLICATE_PROVIDER,
                    Optional.empty(),
                    "More than one Echo Trigger authoring service provider responded.");
        }
        return new TriggerAuthoringServiceLookupResult(
                TriggerAuthoringServiceLookupResult.Status.RESOLVED,
                Optional.of(event.providers().get(0)),
                "Echo Trigger authoring service resolved.");
    }
}
