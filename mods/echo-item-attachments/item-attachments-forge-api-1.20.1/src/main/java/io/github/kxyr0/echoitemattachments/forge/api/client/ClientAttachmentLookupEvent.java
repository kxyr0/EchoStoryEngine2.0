package io.github.kxyr0.echoitemattachments.forge.api.client;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class ClientAttachmentLookupEvent extends Event {
    private final AttachmentSubjectRef subject;
    private final AttachmentPointId point;
    private final List<ClientAttachment> attachments = new ArrayList<>();

    public ClientAttachmentLookupEvent(AttachmentSubjectRef subject, AttachmentPointId point) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.point = Objects.requireNonNull(point, "point");
    }

    public AttachmentSubjectRef subject() { return subject; }
    public AttachmentPointId point() { return point; }
    public void register(ClientAttachment attachment) {
        attachments.add(Objects.requireNonNull(attachment, "attachment"));
    }
    public List<ClientAttachment> attachments() { return List.copyOf(attachments); }
}
