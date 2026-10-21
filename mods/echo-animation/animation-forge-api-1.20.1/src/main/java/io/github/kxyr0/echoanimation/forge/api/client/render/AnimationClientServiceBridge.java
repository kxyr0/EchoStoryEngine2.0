package io.github.kxyr0.echoanimation.forge.api.client.render;

import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

/**
 * Explicit public client bridge for resolving Echo Animation presentation.
 */
public final class AnimationClientServiceBridge {
    private AnimationClientServiceBridge() {
    }

    public static Result lookup() {
        AnimationClientServiceLookupEvent event = new AnimationClientServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new Result(Status.UNAVAILABLE, Optional.empty(), "Echo Animation client service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new Result(Status.DUPLICATE_PROVIDER, Optional.empty(), "More than one provider responded.");
        }
        return new Result(Status.RESOLVED, Optional.of(event.providers().get(0)),
                "Echo Animation client service resolved.");
    }

    public record Result(Status status, Optional<AnimationClientService> service, String message) {
        public Result {
            status = java.util.Objects.requireNonNull(status, "status");
            service = java.util.Objects.requireNonNull(service, "service");
            message = java.util.Objects.requireNonNull(message, "message");
        }
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE,
        DUPLICATE_PROVIDER
    }
}
