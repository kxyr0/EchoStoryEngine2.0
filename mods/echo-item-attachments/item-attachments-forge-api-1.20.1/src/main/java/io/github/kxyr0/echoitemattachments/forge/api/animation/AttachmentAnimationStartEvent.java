package io.github.kxyr0.echoitemattachments.forge.api.animation;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.forge.api.service.PlaybackBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class AttachmentAnimationStartEvent extends Event {
    private final MinecraftServer server;
    private final AttachmentCommandId commandId;
    private final AttachmentSubjectRef subject;
    private final String animationId;
    private final String markerId;
    private final List<PlaybackBinding> bindings = new ArrayList<>();

    public AttachmentAnimationStartEvent(MinecraftServer server, AttachmentCommandId commandId,
            AttachmentSubjectRef subject, String animationId, String markerId) {
        this.server = Objects.requireNonNull(server, "server");
        this.commandId = Objects.requireNonNull(commandId, "command id");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.animationId = Objects.requireNonNull(animationId, "animation id");
        this.markerId = Objects.requireNonNull(markerId, "marker id");
    }

    public MinecraftServer server() { return server; }
    public AttachmentCommandId commandId() { return commandId; }
    public AttachmentSubjectRef subject() { return subject; }
    public String animationId() { return animationId; }
    public String markerId() { return markerId; }
    public void bind(PlaybackBinding binding) { bindings.add(Objects.requireNonNull(binding)); }
    public List<PlaybackBinding> bindings() { return List.copyOf(bindings); }
}
