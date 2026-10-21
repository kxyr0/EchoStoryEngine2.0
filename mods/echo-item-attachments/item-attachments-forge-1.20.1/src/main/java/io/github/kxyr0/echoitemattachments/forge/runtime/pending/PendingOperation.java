package io.github.kxyr0.echoitemattachments.forge.runtime.pending;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;

public sealed interface PendingOperation permits PendingTake, PendingMove, PendingRelease {
    AttachmentCommandId commandId();

    AttachmentSubjectRef subject();

    AttachmentPointId point();

    String ownerId();
}
