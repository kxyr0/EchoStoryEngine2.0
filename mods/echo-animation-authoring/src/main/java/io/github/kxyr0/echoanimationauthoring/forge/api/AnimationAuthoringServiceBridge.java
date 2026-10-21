package io.github.kxyr0.echoanimationauthoring.forge.api;

import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

public final class AnimationAuthoringServiceBridge {
    private AnimationAuthoringServiceBridge() {
    }

    public static AnimationAuthoringServiceLookupResult lookup() {
        AnimationAuthoringServiceLookupEvent event = new AnimationAuthoringServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new AnimationAuthoringServiceLookupResult(
                    AnimationAuthoringServiceLookupResult.Status.UNAVAILABLE,
                    Optional.empty(), "Echo Animation Authoring service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new AnimationAuthoringServiceLookupResult(
                    AnimationAuthoringServiceLookupResult.Status.DUPLICATE_PROVIDER,
                    Optional.empty(), "More than one Echo Animation Authoring provider responded.");
        }
        return new AnimationAuthoringServiceLookupResult(
                AnimationAuthoringServiceLookupResult.Status.RESOLVED,
                Optional.of(event.providers().get(0)), "Echo Animation Authoring service resolved.");
    }
}
