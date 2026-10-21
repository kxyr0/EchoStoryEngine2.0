package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.command.TakeItemCommand;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.forge.api.animation.AttachmentAnimationBridge;
import io.github.kxyr0.echoitemattachments.forge.api.subject.ApproachStatus;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectAccess;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectBridge;
import io.github.kxyr0.echoitemattachments.forge.persistence.ItemStackCodec;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingTake;
import io.github.kxyr0.echoitemattachments.forge.runtime.source.HandOccupancy;
import io.github.kxyr0.echoitemattachments.forge.runtime.source.SourceItemResolver;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

final class TakeFlow {
    private final ServerAttachmentHost host;
    private final SourceItemResolver sources = new SourceItemResolver();
    private final HandOccupancy occupancy = new HandOccupancy();
    private final ItemStackCodec stacks = new ItemStackCodec();

    TakeFlow(ServerAttachmentHost host) {
        this.host = host;
    }

    AttachmentCommandResult start(TakeItemCommand command) {
        Optional<io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingOperation> existing =
                host.pending(command.commandId());
        if (existing.isPresent()) return host.commands().find(command.commandId()).orElseThrow();
        host.leases().acquire(command.subject(), command.point(), command.commandId(), command.ownerId());
        AttachmentSubjectAccess subject = subject(command);
        if (!subject.authorises(command.ownerId())) return fail(command, "EIA-SUBJECT-UNAUTHORISED",
                "Owner is not authorised to control the subject.");
        if (occupancy.occupied(subject.entity(), command.point())) return fail(command, "EIA-VANILLA-HAND-OCCUPIED",
                "Physical vanilla hand already contains an item.");
        ApproachStatus approach = subject.approach(command.commandId(), command.source(),
                command.approachDistance(), command.timeoutTicks(), command.ownerId());
        if (approach == ApproachStatus.ARRIVED) return prepare(command, subject);
        if (approach != ApproachStatus.RUNNING) return fail(command, code(approach),
                "Subject could not approach the authored item source.");
        PendingTake pending = new PendingTake(command, PendingTake.Stage.APPROACHING,
                Optional.empty(), host.server().getTickCount());
        host.putPending(pending);
        return host.commands().running(command.commandId(), "EIA-TAKE-APPROACHING",
                "Subject is approaching the authored item source.");
    }

    void progress(PendingTake pending) {
        try {
            AttachmentSubjectAccess subject = subject(pending.command());
            ApproachStatus status = subject.approach(pending.commandId(), pending.command().source(),
                    pending.command().approachDistance(), pending.command().timeoutTicks(), pending.ownerId());
            if (status == ApproachStatus.RUNNING) return;
            if (status == ApproachStatus.ARRIVED) {
                prepare(pending.command(), subject);
                return;
            }
            fail(pending.command(), code(status), "Approach did not complete.");
        } catch (RuntimeException exception) {
            fail(pending.command(), "EIA-TAKE-PROGRESS", exception.getMessage());
        }
    }

    AttachmentCommandResult attach(PendingTake pending) {
        var record = host.attachments().find(pending.attachmentId().orElseThrow()).orElseThrow();
        ItemEntity source = sourceEntity(record.sourceEntityId().orElseThrow());
        ItemStack live = source.getItem();
        String liveId = BuiltInRegistries.ITEM.getKey(live.getItem()).toString();
        ItemStack payload = stacks.decode(record.stackTag());
        if (!liveId.equals(record.itemId()) || live.getCount() < record.count()
                || !ItemStack.isSameItemSameTags(live, payload)) {
            return failReserved(pending, "EIA-SOURCE-CHANGED", "Source item changed before attach marker.");
        }
        var subject = subject(pending.command());
        if (subject.entity().distanceToSqr(source) > pending.command().approachDistance()
                * pending.command().approachDistance()) {
            return failReserved(pending, "EIA-SOURCE-OUT-OF-REACH",
                    "Reserved source moved outside the final reach bound.");
        }
        host.attachments().beginAttach(record.attachmentId());
        host.persist();
        live.shrink(record.count());
        if (live.isEmpty()) source.discard(); else source.setItem(live);
        host.attachments().completeAttach(record.attachmentId());
        host.persist();
        host.sync(record.subject());
        host.finish(pending);
        return host.commands().succeed(pending.commandId(), "Real ItemStack attached to hand.");
    }

    void cleanup(PendingTake pending) {
        pending.attachmentId().flatMap(host.attachments()::find)
                .filter(record -> record.state() == ItemCustodyState.WORLD_ITEM)
                .ifPresent(record -> host.attachments().abandonWorld(record.attachmentId()));
        AttachmentSubjectBridge.lookup(host.server(), pending.subject()).ifPresent(subject ->
                subject.cancelApproach(pending.commandId(), pending.ownerId()));
    }

    private AttachmentCommandResult prepare(TakeItemCommand command, AttachmentSubjectAccess subject) {
        var selection = sources.resolve(host.server(), command.source(), command.selector());
        if (selection.status() != io.github.kxyr0.echoitemattachments.forge.runtime.source.SourceSelection.Status.RESOLVED) {
            return fail(command, selection.diagnosticCode(), selection.message());
        }
        ItemEntity source = selection.entity().orElseThrow();
        ItemStack payload = source.getItem().copyWithCount(command.selector().count());
        var id = AttachmentIds.attachment();
        host.attachments().reserveWorld(id, AttachmentIds.custody(), command.subject(), command.point(),
                BuiltInRegistries.ITEM.getKey(payload.getItem()).toString(), payload.getCount(),
                source.getItem().getCount(), stacks.encode(payload), source.getUUID().toString());
        host.persist();
        PendingTake pending = new PendingTake(command, PendingTake.Stage.WAITING_FOR_ATTACH_MARKER,
                Optional.of(id), host.server().getTickCount());
        host.putPending(pending);
        if (command.animation().animationId().isEmpty()) return attach(pending);
        var binding = AttachmentAnimationBridge.start(host.server(), command.commandId(), command.subject(),
                command.animation().animationId().orElseThrow(), command.animation().markerId());
        if (binding.isEmpty()) return failReserved(pending, "EIA-ANIMATION-UNAVAILABLE",
                "Animation adapter did not bind a playback.");
        host.bindPlayback(binding.orElseThrow());
        return host.commands().running(command.commandId(), "EIA-TAKE-WAITING-MARKER",
                "Source remains in world until the exact attach marker.");
    }

    private AttachmentSubjectAccess subject(TakeItemCommand command) {
        return AttachmentSubjectBridge.lookup(host.server(), command.subject()).orElseThrow(() ->
                new IllegalStateException("EIA-SUBJECT-UNAVAILABLE: subject adapter unavailable"));
    }

    private ItemEntity sourceEntity(String uuid) {
        UUID id = UUID.fromString(uuid);
        for (ServerLevel level : host.server().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof ItemEntity item && item.isAlive()) return item;
        }
        throw new IllegalStateException("EIA-SOURCE-MISSING: reserved world item no longer exists");
    }

    private AttachmentCommandResult fail(TakeItemCommand command, String code, String message) {
        host.leases().release(command.subject(), command.point(), command.commandId());
        host.pending(command.commandId()).ifPresent(host::finish);
        return host.commands().fail(command.commandId(), code, message == null ? code : message);
    }

    private AttachmentCommandResult failReserved(PendingTake pending, String code, String message) {
        pending.attachmentId().ifPresent(id -> host.attachments().abandonWorld(id));
        host.persist();
        return fail(pending.command(), code, message);
    }

    private String code(ApproachStatus status) {
        return "EIA-APPROACH-" + status.name();
    }
}
