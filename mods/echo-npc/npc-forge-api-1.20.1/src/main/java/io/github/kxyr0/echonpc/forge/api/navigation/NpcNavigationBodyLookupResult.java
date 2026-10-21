package io.github.kxyr0.echonpc.forge.api.navigation;

import java.util.Objects;
import java.util.Optional;

public record NpcNavigationBodyLookupResult(Status status, Optional<NpcNavigationBody> body, String message) {
    public NpcNavigationBodyLookupResult {
        status = Objects.requireNonNull(status, "status");
        body = Objects.requireNonNull(body, "body");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        RESOLVED,
        NOT_FOUND,
        NOT_MATERIALIZED,
        DUPLICATE_PROVIDER
    }
}
