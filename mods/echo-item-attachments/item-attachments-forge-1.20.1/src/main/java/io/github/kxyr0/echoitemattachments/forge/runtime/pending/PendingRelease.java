package io.github.kxyr0.echoitemattachments.forge.runtime.pending;

import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.TransitionKind;
import java.util.Objects;

public record PendingRelease(ReleaseItemCommand command, AttachmentId attachmentId,
        TransitionKind kind) implements PendingOperation {
    public PendingRelease {
        command = Objects.requireNonNull(command, "command");
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
        kind = Objects.requireNonNull(kind, "kind");
        if (kind != TransitionKind.DROP && kind != TransitionKind.THROW) {
            throw new IllegalArgumentException("release kind must be DROP or THROW");
        }
    }

    @Override public AttachmentCommandId commandId() { return command.commandId(); }
    @Override public AttachmentSubjectRef subject() { return command.subject(); }
    @Override public AttachmentPointId point() { return command.point(); }
    @Override public String ownerId() { return command.ownerId(); }
}
