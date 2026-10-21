package io.github.kxyr0.echodialoguenpcadapter.npc;

import io.github.kxyr0.echonpc.api.service.NpcService;
import java.util.Objects;
import java.util.Optional;

public record NpcServiceLookup(Status status, Optional<NpcService> service, String message) {
    public NpcServiceLookup {
        status = Objects.requireNonNull(status, "status");
        service = Objects.requireNonNull(service, "service");
        message = Objects.requireNonNull(message, "message").trim();
        if (status == Status.RESOLVED && service.isEmpty()) {
            throw new IllegalArgumentException("RESOLVED lookup requires service.");
        }
        if (status != Status.RESOLVED && service.isPresent()) {
            throw new IllegalArgumentException(status + " lookup must not contain service.");
        }
    }

    public static NpcServiceLookup resolved(NpcService service) {
        return new NpcServiceLookup(Status.RESOLVED, Optional.of(service), "Echo NPC service resolved.");
    }

    public static NpcServiceLookup unavailable(String message) {
        return new NpcServiceLookup(Status.UNAVAILABLE, Optional.empty(), message);
    }

    public static NpcServiceLookup duplicateProvider(String message) {
        return new NpcServiceLookup(Status.DUPLICATE_PROVIDER, Optional.empty(), message);
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE,
        DUPLICATE_PROVIDER
    }
}
