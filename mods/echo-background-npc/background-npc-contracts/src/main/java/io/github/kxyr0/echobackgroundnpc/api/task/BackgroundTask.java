package io.github.kxyr0.echobackgroundnpc.api.task;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import java.util.Objects;
import java.util.Optional;

/** Closed high-level BN1 task set; there is intentionally no generic string action. */
public sealed interface BackgroundTask permits BackgroundTask.Travel, BackgroundTask.Stay,
        BackgroundTask.Perform, BackgroundTask.Converse {
    TaskKind kind();

    record Travel(BackgroundLocationId destination) implements BackgroundTask {
        public Travel { destination = Objects.requireNonNull(destination, "destination"); }
        @Override public TaskKind kind() { return TaskKind.TRAVEL; }
    }

    record Stay(BackgroundLocationId location) implements BackgroundTask {
        public Stay { location = Objects.requireNonNull(location, "location"); }
        @Override public TaskKind kind() { return TaskKind.STAY; }
    }

    record Perform(String animationId, Optional<BackgroundLocationId> location) implements BackgroundTask {
        public Perform {
            animationId = Objects.requireNonNull(animationId, "animationId").trim();
            location = Objects.requireNonNull(location, "location");
            if (!animationId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("animationId must be namespaced.");
            }
        }
        @Override public TaskKind kind() { return TaskKind.PERFORM; }
    }

    record Converse(BackgroundNpcId participant, AmbientDialogueId dialogue,
            Optional<BackgroundLocationId> rendezvous) implements BackgroundTask {
        public Converse {
            participant = Objects.requireNonNull(participant, "participant");
            dialogue = Objects.requireNonNull(dialogue, "dialogue");
            rendezvous = Objects.requireNonNull(rendezvous, "rendezvous");
        }
        @Override public TaskKind kind() { return TaskKind.CONVERSE; }
    }
}
