package io.github.kxyr0.echoevents.api.subscription;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.payload.AnimationFact;
import io.github.kxyr0.echoevents.api.payload.DialogueFact;
import io.github.kxyr0.echoevents.api.payload.NpcFact;
import java.util.Objects;
import java.util.Optional;

public record EventFilter(EventKind kind, ResourceId resource, Optional<SymbolId> detail, Optional<SymbolId> subject) {
    public EventFilter(EventKind kind, ResourceId resource, Optional<SymbolId> detail) {
        this(kind, resource, detail, Optional.empty());
    }
    public EventFilter {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(subject, "subject");
        if (subject.isPresent() && kind != EventKind.ANIMATION_MARKER && kind != EventKind.ANIMATION_COMPLETED
                && !kind.name().startsWith("TELEPORT_")) {
            throw new IllegalArgumentException("Subject filter only applies to Animation/TeleportMagic events");
        }
        boolean requiresDetail = kind == EventKind.DIALOGUE_CHOICE || kind == EventKind.ANIMATION_MARKER
                || kind == EventKind.NPC_ARRIVED;
        if (requiresDetail != detail.isPresent()) throw new IllegalArgumentException("Invalid event filter detail");
    }
    public boolean matches(EventEnvelope<?> event) {
        if (event.kind() != kind || !event.source().resource().equals(resource)) return false;
        if (subject.isPresent()) {
            var actual = event.payload() instanceof AnimationFact animation ? animation.subject()
                    : event.payload() instanceof io.github.kxyr0.echoevents.api.payload.EventPayload.TeleportFact teleport
                            ? teleport.subject() : null;
            if (!subject.get().equals(actual)) return false;
        }
        if (detail.isEmpty()) return true;
        if (event.payload() instanceof DialogueFact dialogue) return dialogue.choice().equals(detail);
        if (event.payload() instanceof AnimationFact animation) return animation.marker().equals(detail);
        if (event.payload() instanceof NpcFact npc) {
            return npc.destination().map(value -> new SymbolId(value.value())).equals(detail);
        }
        return false;
    }
}
