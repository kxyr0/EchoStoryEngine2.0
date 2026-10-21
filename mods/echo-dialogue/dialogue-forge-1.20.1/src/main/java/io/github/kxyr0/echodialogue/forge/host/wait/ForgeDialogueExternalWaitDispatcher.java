package io.github.kxyr0.echodialogue.forge.host.wait;

import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeDialogueExternalWaitDispatcher {
    private final DialoguePublicSessionReadModel readModel;

    public ForgeDialogueExternalWaitDispatcher(DialoguePublicSessionReadModel readModel) {
        this.readModel = readModel;
    }

    public void dispatch(
            Optional<ServerPlayer> player,
            DialogueTransition transition,
            DialogueCheckpoint saved
    ) {
        if (transition.status() == DialogueTransitionStatus.WAITING
                && transition.state().externalWait().isPresent()) {
            MinecraftForge.EVENT_BUS.post(new DialogueExternalWaitDispatchEvent(player,
                    readModel.view(transition.state(), saved.repositoryRevision()),
                    transition.state().externalWait().orElseThrow()));
        }
    }
}
