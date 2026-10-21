package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import java.util.Objects;
import java.util.Optional;

public record NpcFact(SymbolId actor, Optional<ResourceId> destination, Optional<Position> position) implements EventPayload {
    public NpcFact(SymbolId actor, Optional<ResourceId> destination) { this(actor, destination, Optional.empty()); }
    public NpcFact {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(position, "position");
        if (destination.isPresent() && position.isPresent()) throw new IllegalArgumentException("Ambiguous NPC destination");
    }
    public EventKind kind() { return destination.isPresent() || position.isPresent() ? EventKind.NPC_ARRIVED : EventKind.NPC_DIED; }
    public record Position(String dimension, int x, int y, int z) {
        public Position { new ResourceId(dimension); }
    }
}
