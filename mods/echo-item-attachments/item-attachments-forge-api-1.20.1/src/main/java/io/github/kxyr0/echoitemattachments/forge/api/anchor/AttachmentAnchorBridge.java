package io.github.kxyr0.echoitemattachments.forge.api.anchor;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class AttachmentAnchorBridge {
    private AttachmentAnchorBridge() {
    }

    public static Optional<AttachmentAnchorSample> sample(MinecraftServer server,
            AttachmentSubjectRef subject, AttachmentPointId point) {
        AttachmentAnchorSampleEvent event = new AttachmentAnchorSampleEvent(server, subject, point);
        MinecraftForge.EVENT_BUS.post(event);
        return event.samples().size() == 1 ? Optional.of(event.samples().get(0)) : Optional.empty();
    }
}
