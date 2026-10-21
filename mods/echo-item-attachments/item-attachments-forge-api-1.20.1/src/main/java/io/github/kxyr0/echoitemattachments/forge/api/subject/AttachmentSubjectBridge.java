package io.github.kxyr0.echoitemattachments.forge.api.subject;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class AttachmentSubjectBridge {
    private AttachmentSubjectBridge() {
    }

    public static Optional<AttachmentSubjectAccess> lookup(
            MinecraftServer server, AttachmentSubjectRef subject) {
        AttachmentSubjectLookupEvent event = new AttachmentSubjectLookupEvent(server, subject);
        MinecraftForge.EVENT_BUS.post(event);
        return event.accesses().size() == 1 ? Optional.of(event.accesses().get(0)) : Optional.empty();
    }
}
