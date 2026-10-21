package io.github.kxyr0.echoanimationauthoring.forge.api;

import io.github.kxyr0.echoanimationauthoring.api.service.AnimationAuthoringService;
import java.util.Objects;
import java.util.Optional;

public record AnimationAuthoringServiceLookupResult(
        Status status,
        Optional<AnimationAuthoringService> service,
        String message
) {
    public AnimationAuthoringServiceLookupResult {
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
