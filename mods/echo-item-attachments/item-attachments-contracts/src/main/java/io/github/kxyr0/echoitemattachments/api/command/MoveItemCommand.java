package io.github.kxyr0.echoitemattachments.api.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AnimationCue;
import java.util.Objects;

public record MoveItemCommand(
        AttachmentCommandId commandId,
        AttachmentSubjectRef subject,
        AttachmentPointId from,
        AttachmentPointId to,
        String ownerId,
        AnimationCue animation) {
    public MoveItemCommand {
        commandId = Objects.requireNonNull(commandId, "command id");
        subject = Objects.requireNonNull(subject, "subject");
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        if (!from.isHand() || !to.isHand() || from.equals(to)) {
            throw new IllegalArgumentException("move requires two distinct hand points");
        }
        ownerId = Objects.requireNonNull(ownerId, "owner id").trim();
        if (ownerId.isEmpty()) throw new IllegalArgumentException("owner id must be non-empty");
        animation = Objects.requireNonNull(animation, "animation");
    }
}
