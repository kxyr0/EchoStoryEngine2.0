package io.github.kxyr0.echoitemattachments.forge.api.client;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

public final class ClientAttachmentBridge {
    private ClientAttachmentBridge() {
    }

    public static Optional<ClientAttachment> lookup(
            AttachmentSubjectRef subject, AttachmentPointId point) {
        ClientAttachmentLookupEvent event = new ClientAttachmentLookupEvent(subject, point);
        MinecraftForge.EVENT_BUS.post(event);
        return event.attachments().size() == 1
                ? Optional.of(event.attachments().get(0)) : Optional.empty();
    }
}
