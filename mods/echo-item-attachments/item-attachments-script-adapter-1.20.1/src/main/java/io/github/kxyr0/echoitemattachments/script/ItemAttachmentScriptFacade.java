package io.github.kxyr0.echoitemattachments.script;

import io.github.kxyr0.echoitemattachments.api.command.ItemSelector;
import io.github.kxyr0.echoitemattachments.api.command.MoveItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.TakeItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.AnimationCue;
import io.github.kxyr0.echoitemattachments.api.marker.ItemAttachmentMarkers;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentService;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentServiceBridge;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class ItemAttachmentScriptFacade {
    private final MinecraftServer server;

    public ItemAttachmentScriptFacade(MinecraftServer server) {
        this.server = server;
    }

    public AttachmentCommandResult invoke(String operation, String owner, Map<String, String> values) {
        ItemAttachmentScriptPayload payload = new ItemAttachmentScriptPayload(values);
        AttachmentCommandId id = commandId(operation, owner, values);
        AnimationCue cue = cue(operation, payload);
        return switch (operation) {
            case "take" -> service().take(new TakeItemCommand(id, payload.subject(), payload.hand("hand"),
                    payload.position(), new ItemSelector(payload.expected(), payload.count(), payload.radius()),
                    1.25D, 200, owner, cue));
            case "move" -> service().move(new MoveItemCommand(id, payload.subject(), payload.hand("from"),
                    payload.hand("to"), owner, cue));
            case "drop" -> service().drop(release(id, payload, owner, cue));
            case "throw" -> service().throwItem(release(id, payload, owner, cue));
            default -> throw new IllegalArgumentException("Unknown item attachment operation: " + operation);
        };
    }

    public Optional<AttachmentCommandResult> command(AttachmentCommandId commandId) {
        return service().command(commandId);
    }

    public AttachmentCommandResult cancel(AttachmentCommandId commandId) {
        return service().cancel(commandId);
    }

    public boolean hasAttachedItem(io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef subject,
            io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId point) {
        return service().hasAttachedItem(subject, point);
    }

    public boolean isHoldingItem(io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef subject,
            io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId point, String itemId) {
        return service().isHoldingItem(subject, point, itemId);
    }

    public boolean hasItemTransition(
            io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef subject) {
        return service().hasItemTransition(subject);
    }

    private ReleaseItemCommand release(AttachmentCommandId id, ItemAttachmentScriptPayload payload,
            String owner, AnimationCue cue) {
        return new ReleaseItemCommand(id, payload.subject(), payload.hand("hand"), payload.release(),
                0.0D, 0.15D, 1.0D, payload.speed(), owner, cue);
    }

    private AnimationCue cue(String operation, ItemAttachmentScriptPayload payload) {
        String marker = switch (operation) {
            case "take" -> payload.hand("hand").equals(
                    io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId.RIGHT_HAND)
                    ? ItemAttachmentMarkers.ATTACH_RIGHT_HAND : ItemAttachmentMarkers.ATTACH_LEFT_HAND;
            case "move" -> payload.hand("to").equals(
                    io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId.RIGHT_HAND)
                    ? ItemAttachmentMarkers.ATTACH_RIGHT_HAND : ItemAttachmentMarkers.ATTACH_LEFT_HAND;
            default -> ItemAttachmentMarkers.RELEASE;
        };
        return new AnimationCue(payload.animation(), marker);
    }

    private AttachmentCommandId commandId(String operation, String owner, Map<String, String> values) {
        UUID value = UUID.nameUUIDFromBytes((operation + "|" + owner + "|" + new java.util.TreeMap<>(values))
                .getBytes(StandardCharsets.UTF_8));
        return new AttachmentCommandId("echo_item_attachments:story/" + value);
    }

    private ItemAttachmentService service() {
        return ItemAttachmentServiceBridge.lookup(server).service().orElseThrow(() ->
                new IllegalStateException("Echo Item Attachments service is unavailable"));
    }
}
