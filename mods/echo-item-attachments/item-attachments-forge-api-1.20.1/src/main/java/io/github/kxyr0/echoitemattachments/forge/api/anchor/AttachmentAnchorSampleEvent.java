package io.github.kxyr0.echoitemattachments.forge.api.anchor;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class AttachmentAnchorSampleEvent extends Event {
    private final MinecraftServer server;
    private final AttachmentSubjectRef subject;
    private final AttachmentPointId point;
    private final List<AttachmentAnchorSample> samples = new ArrayList<>();

    public AttachmentAnchorSampleEvent(MinecraftServer server,
            AttachmentSubjectRef subject, AttachmentPointId point) {
        this.server = Objects.requireNonNull(server, "server");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.point = Objects.requireNonNull(point, "point");
    }

    public MinecraftServer server() { return server; }
    public AttachmentSubjectRef subject() { return subject; }
    public AttachmentPointId point() { return point; }
    public void register(AttachmentAnchorSample sample) { samples.add(Objects.requireNonNull(sample)); }
    public List<AttachmentAnchorSample> samples() { return List.copyOf(samples); }
}
