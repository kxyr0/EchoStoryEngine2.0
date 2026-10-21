package io.github.kxyr0.echodialoguenpcadapter.presentation;

import io.github.kxyr0.echonpc.api.service.NpcActorView;
import java.util.Objects;

final class ActorStatusPolicy {
    private ActorStatusPolicy() {
    }

    static Decision decide(NpcActorView actor) {
        NpcActorView checked = Objects.requireNonNull(actor, "actor");
        if (checked.desiredPresence() == NpcActorView.DesiredPresence.ABSENT) {
            return Decision.NOT_HANDLED;
        }
        if (checked.definitionCompatibility() != NpcActorView.DefinitionCompatibility.COMPATIBLE) {
            return Decision.TEMPORARILY_UNAVAILABLE;
        }
        return switch (checked.status()) {
            case ACTIVE, PLATFORM_MISSING, MATERIALIZED, NPC_SLEEPING, DORMANT_CHUNK_UNLOADED, PENDING_SPAWN,
                    PENDING_MOVE -> Decision.RESOLVABLE;
            case PENDING_REMOVAL, BLOCKED_DEFINITION_MISSING, BLOCKED_DEFINITION_INCOMPATIBLE,
                    BLOCKED_WORLD_MISSING -> Decision.TEMPORARILY_UNAVAILABLE;
            case BLOCKED_CORRUPT_RECORD, PLATFORM_FAILURE, HOST_STOPPED -> Decision.FAILURE;
            default -> Decision.TEMPORARILY_UNAVAILABLE;
        };
    }

    enum Decision {
        RESOLVABLE,
        NOT_HANDLED,
        TEMPORARILY_UNAVAILABLE,
        FAILURE
    }
}
