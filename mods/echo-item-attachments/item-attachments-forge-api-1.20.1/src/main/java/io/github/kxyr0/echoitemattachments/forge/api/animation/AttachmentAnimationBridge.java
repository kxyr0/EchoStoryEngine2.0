package io.github.kxyr0.echoitemattachments.forge.api.animation;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.forge.api.service.PlaybackBinding;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class AttachmentAnimationBridge {
    private AttachmentAnimationBridge() {
    }

    public static Optional<PlaybackBinding> start(MinecraftServer server,
            AttachmentCommandId commandId, AttachmentSubjectRef subject,
            String animationId, String markerId) {
        AttachmentAnimationStartEvent event = new AttachmentAnimationStartEvent(
                server, commandId, subject, animationId, markerId);
        MinecraftForge.EVENT_BUS.post(event);
        return event.bindings().size() == 1 ? Optional.of(event.bindings().get(0)) : Optional.empty();
    }
}
