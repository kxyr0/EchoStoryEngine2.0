package io.github.kxyr0.echonpc.api.form.definition;

import io.github.kxyr0.echonpc.api.form.NpcFormCommandId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import java.util.Optional;

public record NpcFormChangeRequest(
        ActorId npcId,
        NpcFormId formId,
        NpcFormTransition transition,
        Optional<NpcFormCommandId> commandId
) {
    public NpcFormChangeRequest {
        npcId = Objects.requireNonNull(npcId, "npcId");
        formId = Objects.requireNonNull(formId, "formId");
        transition = Objects.requireNonNull(transition, "transition");
        commandId = Objects.requireNonNull(commandId, "commandId");
    }

    public NpcFormChangeRequest(ActorId npcId, NpcFormId formId) {
        this(npcId, formId, NpcFormTransition.INSTANT, Optional.empty());
    }

    public NpcFormChangeRequest(ActorId npcId, NpcFormId formId, NpcFormCommandId commandId) {
        this(npcId, formId, NpcFormTransition.INSTANT,
                Optional.of(Objects.requireNonNull(commandId, "commandId")));
    }
}
