package io.github.kxyr0.echoitemattachments.forge.runtime.pending;

import io.github.kxyr0.echoitemattachments.api.command.MoveItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Objects;

public record PendingMove(MoveItemCommand command, AttachmentId attachmentId)
        implements PendingOperation {
    public PendingMove {
        command = Objects.requireNonNull(command, "command");
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
    }

    @Override public AttachmentCommandId commandId() { return command.commandId(); }
    @Override public AttachmentSubjectRef subject() { return command.subject(); }
    @Override public AttachmentPointId point() { return command.to(); }
    @Override public String ownerId() { return command.ownerId(); }
}
