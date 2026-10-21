package io.github.kxyr0.echonpcinteraction.forge.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.forge.api.view.NpcInteractionDiagnosticsView;
import java.util.List;
import java.util.Optional;

public interface NpcInteractionQueryService {
    Optional<NpcInteractionResult> active(ActorId npcId);

    default boolean isInteracting(ActorId npcId) {
        return active(npcId).filter(result -> !result.status().terminal()).isPresent();
    }

    Optional<NpcInteractionResult> command(NpcInteractionCommandId commandId);

    List<NpcInteractionResult> list(int limit);

    NpcInteractionDiagnosticsView diagnostics();
}
