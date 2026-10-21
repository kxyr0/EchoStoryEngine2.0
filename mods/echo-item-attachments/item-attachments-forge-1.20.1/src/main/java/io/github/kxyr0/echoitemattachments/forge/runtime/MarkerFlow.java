package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.core.marker.MarkerDecision;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingMove;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingRelease;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingTake;

final class MarkerFlow {
    private final ServerAttachmentHost host;

    MarkerFlow(ServerAttachmentHost host) {
        this.host = host;
    }

    AttachmentCommandResult accept(AttachmentCommandId commandId, MarkerSignal signal) {
        MarkerDecision decision = host.markers().accept(commandId, signal);
        if (decision.status() == MarkerDecision.Status.DUPLICATE) {
            return host.commands().find(commandId).orElseThrow();
        }
        if (decision.status() != MarkerDecision.Status.ACCEPTED) {
            return host.commands().running(commandId, decision.diagnosticCode(),
                    "Marker ignored because it was stale, mismatched, or unbound.");
        }
        var operation = host.pending(commandId).orElseThrow(() ->
                new IllegalStateException("EIA-MARKER-NO-TRANSITION: command is not pending"));
        if (operation instanceof PendingTake take) return host.takes().attach(take);
        if (operation instanceof PendingMove move) return host.moves().complete(move);
        if (operation instanceof PendingRelease release) return host.releases().complete(release);
        throw new IllegalStateException("EIA-MARKER-UNKNOWN-TRANSITION");
    }
}
