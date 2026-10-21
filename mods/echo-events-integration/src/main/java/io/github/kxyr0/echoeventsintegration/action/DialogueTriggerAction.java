package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class DialogueTriggerAction {
    Acceptance start(TriggerEventActionDispatchEvent action, ServerPlayer player) {
        var lookup = new EchoDialogueServiceLookupEvent(action.server());
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().size() != 1) return Acceptance.RETRY;
        var service = lookup.providers().get(0);
        var session = new DialogueSessionId(action.actionToken());
        var existing = service.query(session).session();
        if (existing.isPresent() && service.isDurablyAccepted(session)) {
            var accepted = existing.get();
            boolean same = accepted.audienceId().value().equals(player.getUUID().toString())
                    && ActionResources.canonical(accepted.dialogueId().toString()).equals(ActionResources.target(action));
            return same ? Acceptance.ACCEPTED : Acceptance.REJECTED;
        }
        var result = service.start(new DialogueStartRequest(session, DialogueId.parse(ActionResources.target(action)),
                new AudienceId(player.getUUID().toString()), java.util.Optional.of(new DialogueStartRequest.Origin(
                        action.delivery().event().source().rootExecution().value(), action.delivery().executionToken()))));
        return switch (result.status()) {
            case ACCEPTED, STARTED, ALREADY_ACTIVE, WAITING, SERVICE_UNAVAILABLE, CONFLICT -> Acceptance.RETRY;
            case ALREADY_TERMINAL -> service.isDurablyAccepted(session)
                    ? Acceptance.ACCEPTED : Acceptance.RETRY;
            default -> Acceptance.REJECTED;
        };
    }
}
