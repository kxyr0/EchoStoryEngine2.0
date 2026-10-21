package io.github.kxyr0.echotrigger.api.event;

import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TriggerEvent(
        TriggerEventKind kind,
        Optional<TriggerArea> area,
        TriggerOwnerId owner,
        long serverTick,
        String expressionName,
        Map<String, String> payload
) {
    public TriggerEvent {
        kind = Objects.requireNonNull(kind, "kind");
        area = Objects.requireNonNull(area, "area");
        owner = Objects.requireNonNull(owner, "owner");
        expressionName = Objects.requireNonNull(expressionName, "expressionName").trim();
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        if (serverTick < 0) {
            throw new IllegalArgumentException("serverTick must not be negative.");
        }
        if (expressionName.isEmpty()) {
            expressionName = kind.name();
        }
    }

    public TriggerEvent(TriggerEventKind kind, Optional<TriggerArea> area, TriggerOwnerId owner, long serverTick) {
        this(kind, area, owner, serverTick, kind.name(), Map.of());
    }

    public static TriggerEvent vanilla(
            TriggerOwnerId owner,
            long serverTick,
            String expressionName,
            Map<String, String> payload
    ) {
        return new TriggerEvent(TriggerEventKind.VANILLA_GAMEPLAY, Optional.empty(), owner, serverTick,
                expressionName, payload);
    }

    public static TriggerEvent predicateTrue(
            TriggerOwnerId owner,
            long serverTick,
            String expressionName,
            Map<String, String> payload
    ) {
        return new TriggerEvent(TriggerEventKind.PREDICATE_TRUE, Optional.empty(), owner, serverTick,
                expressionName, payload);
    }
}
