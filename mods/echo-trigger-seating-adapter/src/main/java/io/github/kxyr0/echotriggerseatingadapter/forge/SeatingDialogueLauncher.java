package io.github.kxyr0.echotriggerseatingadapter.forge;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SeatingDialogueLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echotriggerseatingadapter");

    boolean start(TriggerStorySubscription subscription, PlayerSeatEvent event, MinecraftServer server) {
        Optional<DialogueService> service = dialogue(server);
        if (service.isEmpty()) {
            LOGGER.warn("Echo Dialogue unavailable for seating dialogue {}", subscription.dialogueId());
            return false;
        }
        var result = service.orElseThrow().start(new DialogueStartRequest(
                session(event, subscription),
                DialogueId.parse(subscription.dialogueId()),
                new AudienceId(event.player().uuid().toString())));
        boolean accepted = accepted(result.status());
        if (!accepted) {
            LOGGER.warn("Seating dialogue did not start: dialogue={} status={} event={}",
                    subscription.dialogueId(), result.status(), event.eventId().value());
        }
        return accepted;
    }

    private Optional<DialogueService> dialogue(MinecraftServer server) {
        EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        List<DialogueService> providers = lookup.providers();
        if (providers.size() > 1) {
            LOGGER.warn("Multiple Echo Dialogue services found; using the first provider.");
        }
        return providers.stream().findFirst();
    }

    private boolean accepted(DialogueOperationStatus status) {
        return status == DialogueOperationStatus.ACCEPTED
                || status == DialogueOperationStatus.ALREADY_ACTIVE
                || status == DialogueOperationStatus.WAITING
                || status == DialogueOperationStatus.ALREADY_TERMINAL;
    }

    private DialogueSessionId session(PlayerSeatEvent event, TriggerStorySubscription subscription) {
        return new DialogueSessionId("seating-dialogue/" + safe(event.eventId().value())
                + "/" + safe(subscription.subscriptionId().value()));
    }

    private String safe(String value) {
        return value.replace(':', '_').replace('/', '_');
    }
}
