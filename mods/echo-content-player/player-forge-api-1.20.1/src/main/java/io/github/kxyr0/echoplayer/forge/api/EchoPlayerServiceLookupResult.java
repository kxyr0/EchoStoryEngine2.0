package io.github.kxyr0.echoplayer.forge.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of resolving the public server-scoped Echo player service.
 */
public record EchoPlayerServiceLookupResult(
        Status status,
        Optional<EchoPlayerService> service,
        String message
) {
    public EchoPlayerServiceLookupResult {
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
