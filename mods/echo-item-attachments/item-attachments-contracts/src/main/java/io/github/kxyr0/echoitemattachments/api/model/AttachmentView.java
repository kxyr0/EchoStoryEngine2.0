package io.github.kxyr0.echoitemattachments.api.model;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import java.util.Objects;
import java.util.Optional;

public record AttachmentView(
        AttachmentId attachmentId,
        CustodyId custodyId,
        AttachmentSubjectRef subject,
        AttachmentPointId point,
        String itemId,
        int count,
        ItemCustodyState custodyState,
        long revision,
        Optional<String> transitionId) {
    public AttachmentView {
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
        custodyId = Objects.requireNonNull(custodyId, "custody id");
        subject = Objects.requireNonNull(subject, "subject");
        point = Objects.requireNonNull(point, "point");
        itemId = Objects.requireNonNull(itemId, "item id");
        if (count < 1) throw new IllegalArgumentException("count must be positive");
        custodyState = Objects.requireNonNull(custodyState, "custody state");
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        transitionId = Objects.requireNonNull(transitionId, "transition id");
    }
}
