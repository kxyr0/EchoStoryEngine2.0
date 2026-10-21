package io.github.kxyr0.echoitemattachments.forge.runtime.pending;

import io.github.kxyr0.echoitemattachments.api.command.TakeItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Objects;
import java.util.Optional;

public record PendingTake(TakeItemCommand command, Stage stage,
        Optional<AttachmentId> attachmentId, long acceptedTick) implements PendingOperation {
    public PendingTake {
        command = Objects.requireNonNull(command, "command");
        stage = Objects.requireNonNull(stage, "stage");
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
        if (acceptedTick < 0) throw new IllegalArgumentException("accepted tick must be non-negative");
    }

    @Override public AttachmentCommandId commandId() { return command.commandId(); }
    @Override public AttachmentSubjectRef subject() { return command.subject(); }
    @Override public AttachmentPointId point() { return command.point(); }
    @Override public String ownerId() { return command.ownerId(); }

    public enum Stage {
        APPROACHING,
        WAITING_FOR_ATTACH_MARKER
    }
}
