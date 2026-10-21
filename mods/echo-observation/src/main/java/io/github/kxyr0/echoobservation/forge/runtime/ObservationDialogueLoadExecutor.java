package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echoobservation.api.action.ObservationActionContext;
import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutor;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class ObservationDialogueLoadExecutor implements ObservationActionExecutor {
    private final MinecraftServer server;

    ObservationDialogueLoadExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String target() {
        return "Dialogue.Load";
    }

    @Override
    public boolean execute(ObservationActionContext context, ObservationScriptAction action) {
        if (action.arguments().size() != 1) return false;
        try {
            EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
            MinecraftForge.EVENT_BUS.post(lookup);
            if (lookup.providers().size() != 1) return false;
            DialogueStartRequest request = new DialogueStartRequest(
                    new DialogueSessionId("observation-action/" + context.executionId().value()),
                    DialogueId.parse(action.arguments().get(0).value()),
                    new AudienceId(context.playerId().toString()));
            DialogueOperationStatus status = lookup.providers().get(0).start(request).status();
            return status == DialogueOperationStatus.ACCEPTED || status == DialogueOperationStatus.STARTED
                    || status == DialogueOperationStatus.ALREADY_ACTIVE || status == DialogueOperationStatus.WAITING;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
