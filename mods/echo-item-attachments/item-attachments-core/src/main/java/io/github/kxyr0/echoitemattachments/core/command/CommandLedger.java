package io.github.kxyr0.echoitemattachments.core.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.CommandStatus;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CommandLedger {
    private final Map<AttachmentCommandId, AttachmentCommandResult> results = new LinkedHashMap<>();

    public synchronized AttachmentCommandResult accept(AttachmentCommandId id) {
        AttachmentCommandResult prior = results.get(id);
        if (prior != null) return prior;
        return put(id, CommandStatus.ACCEPTED, "EIA-COMMAND-ACCEPTED", "Command accepted.");
    }

    public synchronized AttachmentCommandResult running(AttachmentCommandId id, String code, String message) {
        return terminalGuard(id, CommandStatus.RUNNING, code, message);
    }

    public synchronized AttachmentCommandResult succeed(AttachmentCommandId id, String message) {
        return terminalGuard(id, CommandStatus.SUCCEEDED, "EIA-COMMAND-SUCCEEDED", message);
    }

    public synchronized AttachmentCommandResult fail(AttachmentCommandId id, String code, String message) {
        return terminalGuard(id, CommandStatus.FAILED, code, message);
    }

    public synchronized AttachmentCommandResult cancel(AttachmentCommandId id, String message) {
        return terminalGuard(id, CommandStatus.CANCELLED, "EIA-COMMAND-CANCELLED", message);
    }

    public synchronized AttachmentCommandResult conflict(AttachmentCommandId id) {
        long revision = Optional.ofNullable(results.get(id)).map(AttachmentCommandResult::revision).orElse(0L) + 1L;
        return new AttachmentCommandResult(id, CommandStatus.FAILED, "EIA-IDEMPOTENCY-CONFLICT",
                "Command id was reused with different arguments.", revision);
    }

    public synchronized Optional<AttachmentCommandResult> find(AttachmentCommandId id) {
        return Optional.ofNullable(results.get(id));
    }

    private AttachmentCommandResult terminalGuard(AttachmentCommandId id, CommandStatus status,
            String code, String message) {
        AttachmentCommandResult current = results.get(id);
        if (current != null && current.terminal()) return current;
        return put(id, status, code, message);
    }

    private AttachmentCommandResult put(AttachmentCommandId id, CommandStatus status,
            String code, String message) {
        long revision = Optional.ofNullable(results.get(id)).map(AttachmentCommandResult::revision).orElse(0L) + 1L;
        AttachmentCommandResult result = new AttachmentCommandResult(id, status, code, message, revision);
        results.put(id, result);
        while (results.size() > 4096) results.remove(results.keySet().iterator().next());
        return result;
    }
}
