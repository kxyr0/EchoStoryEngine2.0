package io.github.kxyr0.echoitemattachments.forge.api.subject;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class AttachmentSubjectLookupEvent extends Event {
    private final MinecraftServer server;
    private final AttachmentSubjectRef subject;
    private final List<AttachmentSubjectAccess> accesses = new ArrayList<>();

    public AttachmentSubjectLookupEvent(MinecraftServer server, AttachmentSubjectRef subject) {
        this.server = Objects.requireNonNull(server, "server");
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    public MinecraftServer server() {
        return server;
    }

    public AttachmentSubjectRef subject() {
        return subject;
    }

    public void register(AttachmentSubjectAccess access) {
        accesses.add(Objects.requireNonNull(access, "access"));
    }

    public List<AttachmentSubjectAccess> accesses() {
        return List.copyOf(accesses);
    }
}
