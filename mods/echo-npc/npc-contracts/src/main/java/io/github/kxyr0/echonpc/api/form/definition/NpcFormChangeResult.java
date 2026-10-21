package io.github.kxyr0.echonpc.api.form.definition;

import io.github.kxyr0.echonpc.api.form.NpcFormOutcome;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import java.util.Objects;
import java.util.Optional;

public record NpcFormChangeResult(
        NpcFormStatus status,
        NpcFormOutcome outcome,
        Optional<NpcActorView> actor,
        Optional<NpcFormSnapshot> activeForm,
        Optional<String> operationId,
        String message
) {
    public NpcFormChangeResult {
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        actor = Objects.requireNonNull(actor, "actor");
        activeForm = Objects.requireNonNull(activeForm, "activeForm");
        operationId = Objects.requireNonNull(operationId, "operationId");
        message = Objects.requireNonNull(message, "message");
    }

    public NpcFormChangeResult(NpcFormStatus status, NpcFormOutcome outcome,
            Optional<NpcActorView> actor, String message) {
        this(status, outcome, actor, Optional.empty(), Optional.empty(), message);
    }
}
