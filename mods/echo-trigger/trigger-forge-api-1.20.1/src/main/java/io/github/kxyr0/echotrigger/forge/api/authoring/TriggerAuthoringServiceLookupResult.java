package io.github.kxyr0.echotrigger.forge.api.authoring;

import java.util.Objects;
import java.util.Optional;

public record TriggerAuthoringServiceLookupResult(
        Status status,
        Optional<TriggerAuthoringService> service,
        String message
) {
    public TriggerAuthoringServiceLookupResult {
        status = Objects.requireNonNull(status, "status");
        service = Objects.requireNonNull(service, "service");
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Lookup message is required.");
        }
    }

    public boolean resolved() {
        return status == Status.RESOLVED && service.isPresent();
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE,
        DUPLICATE_PROVIDER
    }
}
