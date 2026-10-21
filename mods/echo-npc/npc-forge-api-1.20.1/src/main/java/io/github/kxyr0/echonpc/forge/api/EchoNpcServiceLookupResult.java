package io.github.kxyr0.echonpc.forge.api;

import io.github.kxyr0.echonpc.api.service.NpcService;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for Forge-side public service lookup.
 */
public record EchoNpcServiceLookupResult(Status status, Optional<NpcService> service, String message) {
    public EchoNpcServiceLookupResult {
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
