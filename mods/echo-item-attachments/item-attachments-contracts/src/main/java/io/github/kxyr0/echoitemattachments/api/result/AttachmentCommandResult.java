package io.github.kxyr0.echoitemattachments.api.result;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.CommandStatus;
import java.util.Objects;

public record AttachmentCommandResult(
        AttachmentCommandId commandId,
        CommandStatus status,
        String diagnosticCode,
        String message,
        long revision) {
    public AttachmentCommandResult {
        commandId = Objects.requireNonNull(commandId, "command id");
        status = Objects.requireNonNull(status, "status");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnostic code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        if (diagnosticCode.isEmpty()) throw new IllegalArgumentException("diagnostic code required");
        if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
    }

    public boolean terminal() {
        return status == CommandStatus.SUCCEEDED || status == CommandStatus.FAILED
                || status == CommandStatus.CANCELLED;
    }
}
