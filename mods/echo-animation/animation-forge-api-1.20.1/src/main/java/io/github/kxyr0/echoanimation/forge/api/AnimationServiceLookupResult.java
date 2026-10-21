package io.github.kxyr0.echoanimation.forge.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for server-scoped AnimationService lookup.
 */
public record AnimationServiceLookupResult(Status status, Optional<AnimationService> service, String message) {
    public AnimationServiceLookupResult {
        status = Objects.requireNonNull(status, "status");
        service = Objects.requireNonNull(service, "service");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE,
        DUPLICATE_PROVIDER
    }
}
