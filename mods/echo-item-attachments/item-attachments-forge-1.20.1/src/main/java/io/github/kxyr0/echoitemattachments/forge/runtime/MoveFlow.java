package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.command.MoveItemCommand;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.forge.api.animation.AttachmentAnimationBridge;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectBridge;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingMove;
import io.github.kxyr0.echoitemattachments.forge.runtime.source.HandOccupancy;

final class MoveFlow {
    private final ServerAttachmentHost host;
    private final HandOccupancy occupancy = new HandOccupancy();

    MoveFlow(ServerAttachmentHost host) {
        this.host = host;
    }

    AttachmentCommandResult start(MoveItemCommand command) {
        if (host.pending(command.commandId()).isPresent()) {
            return host.commands().find(command.commandId()).orElseThrow();
        }
        host.leases().acquire(command.subject(), command.from(), command.commandId(), command.ownerId());
        host.leases().acquire(command.subject(), command.to(), command.commandId(), command.ownerId());
        var subject = AttachmentSubjectBridge.lookup(host.server(), command.subject()).orElseThrow(() ->
                new IllegalStateException("EIA-SUBJECT-UNAVAILABLE: subject adapter unavailable"));
        if (!subject.authorises(command.ownerId())) return fail(command, "EIA-SUBJECT-UNAUTHORISED",
                "Owner is not authorised to control the subject.");
        if (occupancy.occupied(subject.entity(), command.to())) return fail(command,
                "EIA-VANILLA-HAND-OCCUPIED", "Target vanilla hand contains an item.");
        var record = host.attachments().attached(command.subject(), command.from()).orElseThrow(() ->
                new IllegalStateException("EIA-ATTACHMENT-NOT-FOUND: source hand is empty"));
        PendingMove pending = new PendingMove(command, record.attachmentId());
        host.putPending(pending);
        if (command.animation().animationId().isEmpty()) return complete(pending);
        var binding = AttachmentAnimationBridge.start(host.server(), command.commandId(), command.subject(),
                command.animation().animationId().orElseThrow(), command.animation().markerId());
        if (binding.isEmpty()) return fail(command, "EIA-ANIMATION-UNAVAILABLE",
                "Animation adapter did not bind a playback.");
        host.bindPlayback(binding.orElseThrow());
        return host.commands().running(command.commandId(), "EIA-MOVE-WAITING-MARKER",
                "Item remains in source hand until exact attach marker.");
    }

    AttachmentCommandResult complete(PendingMove pending) {
        host.attachments().move(pending.attachmentId(), pending.command().to());
        host.persist();
        host.sync(pending.subject());
        host.finish(pending);
        return host.commands().succeed(pending.commandId(), "Item moved between real hand anchors.");
    }

    private AttachmentCommandResult fail(MoveItemCommand command, String code, String message) {
        host.pending(command.commandId()).ifPresent(host::finish);
        host.leases().release(command.subject(), command.from(), command.commandId());
        host.leases().release(command.subject(), command.to(), command.commandId());
        return host.commands().fail(command.commandId(), code, message);
    }
}
