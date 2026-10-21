package io.github.kxyr0.echoitemattachments.forge.api.service;

import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.api.command.MoveItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.TakeItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AttachmentView;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import java.util.List;
import java.util.Optional;

public interface ItemAttachmentService {
    AttachmentCommandResult take(TakeItemCommand command);

    AttachmentCommandResult move(MoveItemCommand command);

    AttachmentCommandResult drop(ReleaseItemCommand command);

    AttachmentCommandResult throwItem(ReleaseItemCommand command);

    AttachmentCommandResult marker(AttachmentCommandId commandId, MarkerSignal signal);

    AttachmentCommandResult bindPlayback(PlaybackBinding binding);

    AttachmentCommandResult cancel(AttachmentCommandId commandId);

    Optional<AttachmentCommandResult> command(AttachmentCommandId commandId);

    List<AttachmentView> inspect(AttachmentSubjectRef subject);

    default boolean hasAttachedItem(AttachmentSubjectRef subject, AttachmentPointId point) {
        return inspect(subject).stream().anyMatch(view -> view.point().equals(point)
                && (view.custodyState() == io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState.ATTACHED_TO_SUBJECT
                || view.custodyState() == io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState.RELEASING));
    }

    default boolean isHoldingItem(AttachmentSubjectRef subject, AttachmentPointId point, String itemId) {
        return inspect(subject).stream().anyMatch(view -> view.point().equals(point)
                && view.itemId().equals(itemId) && hasAttachedItem(subject, point));
    }

    default boolean hasItemTransition(AttachmentSubjectRef subject) {
        return inspect(subject).stream().anyMatch(view -> view.transitionId().isPresent());
    }
}
