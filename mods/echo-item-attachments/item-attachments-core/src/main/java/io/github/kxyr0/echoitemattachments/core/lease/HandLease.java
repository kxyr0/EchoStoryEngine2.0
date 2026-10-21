package io.github.kxyr0.echoitemattachments.core.lease;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Objects;

public record HandLease(
        AttachmentSubjectRef subject,
        AttachmentPointId point,
        AttachmentCommandId commandId,
        String ownerId) {
    public HandLease {
        subject = Objects.requireNonNull(subject, "subject");
        point = Objects.requireNonNull(point, "point");
        commandId = Objects.requireNonNull(commandId, "command id");
        ownerId = Objects.requireNonNull(ownerId, "owner id").trim();
        if (!point.isHand()) throw new IllegalArgumentException("lease point must be a hand");
        if (ownerId.isEmpty()) throw new IllegalArgumentException("owner id must be non-empty");
    }
}
