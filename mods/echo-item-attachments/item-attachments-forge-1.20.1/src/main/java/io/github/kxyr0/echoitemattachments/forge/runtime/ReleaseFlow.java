package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.model.ReleaseMode;
import io.github.kxyr0.echoitemattachments.api.model.TransitionKind;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorBridge;
import io.github.kxyr0.echoitemattachments.forge.api.animation.AttachmentAnimationBridge;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectBridge;
import io.github.kxyr0.echoitemattachments.forge.persistence.ItemStackCodec;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingRelease;
import io.github.kxyr0.echoitemattachments.forge.runtime.release.ReleaseSpawner;
import io.github.kxyr0.echoitemattachments.forge.runtime.release.ReleasePhysics;
import io.github.kxyr0.echoitemattachments.core.model.ReleasePlan;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

final class ReleaseFlow {
    private final ServerAttachmentHost host;
    private final ItemStackCodec stacks = new ItemStackCodec();
    private final ReleaseSpawner spawner = new ReleaseSpawner();
    private final ReleasePhysics physics = new ReleasePhysics();

    ReleaseFlow(ServerAttachmentHost host) {
        this.host = host;
    }

    AttachmentCommandResult start(ReleaseItemCommand command, TransitionKind kind) {
        if (host.pending(command.commandId()).isPresent()) {
            return host.commands().find(command.commandId()).orElseThrow();
        }
        host.leases().acquire(command.subject(), command.point(), command.commandId(), command.ownerId());
        var subject = AttachmentSubjectBridge.lookup(host.server(), command.subject()).orElseThrow(() ->
                new IllegalStateException("EIA-SUBJECT-UNAVAILABLE: subject adapter unavailable"));
        if (!subject.authorises(command.ownerId())) return fail(command, "EIA-SUBJECT-UNAUTHORISED",
                "Owner is not authorised to release this item.");
        var record = host.attachments().attached(command.subject(), command.point()).orElseThrow(() ->
                new IllegalStateException("EIA-ATTACHMENT-NOT-FOUND: hand is empty"));
        PendingRelease pending = new PendingRelease(command, record.attachmentId(), kind);
        host.putPending(pending);
        if (command.animation().animationId().isEmpty()) return complete(pending);
        var binding = AttachmentAnimationBridge.start(host.server(), command.commandId(), command.subject(),
                command.animation().animationId().orElseThrow(), command.animation().markerId());
        if (binding.isEmpty()) return fail(command, "EIA-ANIMATION-UNAVAILABLE",
                "Animation adapter did not bind a playback.");
        host.bindPlayback(binding.orElseThrow());
        return host.commands().running(command.commandId(), "EIA-RELEASE-WAITING-MARKER",
                "Item remains attached until the exact release marker.");
    }

    AttachmentCommandResult complete(PendingRelease pending) {
        var record = host.attachments().find(pending.attachmentId()).orElseThrow();
        var subject = AttachmentSubjectBridge.lookup(host.server(), pending.subject()).orElseThrow(() ->
                new IllegalStateException("EIA-SUBJECT-UNAVAILABLE: release subject unavailable"));
        var anchor = AttachmentAnchorBridge.sample(host.server(), pending.subject(), pending.point())
                .orElseThrow(() -> new IllegalStateException(
                        "EIA-ANCHOR-UNAVAILABLE: no authoritative hand transform sample"));
        var velocity = host.handVelocity(record.attachmentId());
        if (pending.command().mode() == ReleaseMode.HAND_MOTION && velocity.isEmpty()) {
            return fail(pending.command(), "EIA-HAND-MOTION-UNAVAILABLE",
                    "At least two recent server hand samples are required.");
        }
        var releaseId = AttachmentIds.release();
        var releaseVelocity = physics.velocity(pending.command(), subject.entity(), anchor, velocity);
        String dimension = subject.entity().level().dimension().location().toString();
        ReleasePlan plan = new ReleasePlan(dimension, anchor.position().x, anchor.position().y,
                anchor.position().z, releaseVelocity.x, releaseVelocity.y, releaseVelocity.z);
        host.attachments().beginRelease(record.attachmentId(), releaseId, plan);
        host.persist();
        ServerLevel level = (ServerLevel) subject.entity().level();
        spawner.spawnPrepared(level, stacks.decode(record.stackTag()), releaseId, plan);
        host.attachments().confirmReleased(record.attachmentId());
        host.persist();
        host.sync(record.subject());
        host.attachments().forgetReleased(record.attachmentId());
        host.persist();
        host.finish(pending);
        return host.commands().succeed(pending.commandId(),
                pending.kind() == TransitionKind.THROW ? "Item thrown." : "Item dropped.");
    }

    private AttachmentCommandResult fail(ReleaseItemCommand command, String code, String message) {
        host.pending(command.commandId()).ifPresent(host::finish);
        host.leases().release(command.subject(), command.point(), command.commandId());
        return host.commands().fail(command.commandId(), code, message);
    }
}
