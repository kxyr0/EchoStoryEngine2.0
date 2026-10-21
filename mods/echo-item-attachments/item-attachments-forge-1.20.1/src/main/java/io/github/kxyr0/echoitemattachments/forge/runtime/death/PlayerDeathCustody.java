package io.github.kxyr0.echoitemattachments.forge.runtime.death;

import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.api.model.AnimationCue;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.api.model.ReleaseMode;
import io.github.kxyr0.echoitemattachments.api.marker.ItemAttachmentMarkers;
import io.github.kxyr0.echoitemattachments.forge.runtime.ServerAttachmentHost;
import java.util.UUID;

public final class PlayerDeathCustody {
    private final ServerAttachmentHost host;

    public PlayerDeathCustody(ServerAttachmentHost host) {
        this.host = host;
    }

    public void drop(UUID playerId) {
        AttachmentSubjectRef subject = new AttachmentSubjectRef(SubjectKind.PLAYER, playerId.toString());
        host.records().stream().filter(record -> record.subject().equals(subject)
                && record.state() == ItemCustodyState.ATTACHED_TO_SUBJECT).toList().forEach(record ->
                host.drop(new ReleaseItemCommand(
                        new AttachmentCommandId("echo_item_attachments:death/" + UUID.randomUUID()),
                        subject, record.point(), ReleaseMode.LOOK_DIRECTION, 0.0D, 0.0D, 0.0D,
                        0.15D, "echo_item_attachments:death",
                        AnimationCue.immediate(ItemAttachmentMarkers.RELEASE))));
    }
}
