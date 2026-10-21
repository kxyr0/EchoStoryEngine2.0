package io.github.kxyr0.echoitemattachments.api.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AnimationCue;
import io.github.kxyr0.echoitemattachments.api.model.ReleaseMode;
import java.util.Objects;

public record ReleaseItemCommand(
        AttachmentCommandId commandId,
        AttachmentSubjectRef subject,
        AttachmentPointId point,
        ReleaseMode mode,
        double localX,
        double localY,
        double localZ,
        double speed,
        String ownerId,
        AnimationCue animation) {
    public ReleaseItemCommand {
        commandId = Objects.requireNonNull(commandId, "command id");
        subject = Objects.requireNonNull(subject, "subject");
        point = Objects.requireNonNull(point, "point");
        if (!point.isHand()) throw new IllegalArgumentException("point must be a hand");
        mode = Objects.requireNonNull(mode, "mode");
        if (!Double.isFinite(localX) || !Double.isFinite(localY) || !Double.isFinite(localZ)) {
            throw new IllegalArgumentException("release vector must be finite");
        }
        if (!Double.isFinite(speed) || speed < 0.0D || speed > 4.0D) {
            throw new IllegalArgumentException("speed must be in [0, 4]");
        }
        ownerId = Objects.requireNonNull(ownerId, "owner id").trim();
        if (ownerId.isEmpty()) throw new IllegalArgumentException("owner id must be non-empty");
        animation = Objects.requireNonNull(animation, "animation");
    }
}
