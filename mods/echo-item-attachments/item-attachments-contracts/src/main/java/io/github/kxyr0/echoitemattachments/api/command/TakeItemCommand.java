package io.github.kxyr0.echoitemattachments.api.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AnimationCue;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import java.util.Objects;

public record TakeItemCommand(
        AttachmentCommandId commandId,
        AttachmentSubjectRef subject,
        AttachmentPointId point,
        WorldPosition source,
        ItemSelector selector,
        double approachDistance,
        int timeoutTicks,
        String ownerId,
        AnimationCue animation) {
    public TakeItemCommand {
        commandId = Objects.requireNonNull(commandId, "command id");
        subject = Objects.requireNonNull(subject, "subject");
        point = Objects.requireNonNull(point, "point");
        if (!point.isHand()) throw new IllegalArgumentException("point must be a hand");
        source = Objects.requireNonNull(source, "source");
        selector = Objects.requireNonNull(selector, "selector");
        if (!Double.isFinite(approachDistance) || approachDistance <= 0.0D) {
            throw new IllegalArgumentException("approach distance must be positive");
        }
        if (timeoutTicks < 1) throw new IllegalArgumentException("timeout must be positive");
        ownerId = Objects.requireNonNull(ownerId, "owner id").trim();
        if (ownerId.isEmpty()) throw new IllegalArgumentException("owner id must be non-empty");
        animation = Objects.requireNonNull(animation, "animation");
    }
}
