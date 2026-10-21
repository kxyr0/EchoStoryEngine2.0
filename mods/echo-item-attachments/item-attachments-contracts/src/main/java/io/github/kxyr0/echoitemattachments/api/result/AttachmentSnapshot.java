package io.github.kxyr0.echoitemattachments.api.result;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AttachmentView;
import java.util.List;
import java.util.Objects;

public record AttachmentSnapshot(
        AttachmentSubjectRef subject,
        long revision,
        List<AttachmentView> attachments) {
    public AttachmentSnapshot {
        subject = Objects.requireNonNull(subject, "subject");
        if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
        attachments = List.copyOf(attachments);
    }
}
