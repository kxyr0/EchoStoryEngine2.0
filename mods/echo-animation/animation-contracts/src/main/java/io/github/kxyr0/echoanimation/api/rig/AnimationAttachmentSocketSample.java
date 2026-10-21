package io.github.kxyr0.echoanimation.api.rig;

import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;

/**
 * Authoritative server-tick model-space sample of one active rig socket.
 */
public record AnimationAttachmentSocketSample(
        AnimationSubjectId subjectId,
        AnimationPlaybackId playbackId,
        AnimationAttachmentSocketId socketId,
        Matrix4Transform modelTransform,
        long serverTick,
        long playbackRevision) {
    public AnimationAttachmentSocketSample {
        subjectId = Objects.requireNonNull(subjectId, "subject id");
        playbackId = Objects.requireNonNull(playbackId, "playback id");
        socketId = Objects.requireNonNull(socketId, "socket id");
        modelTransform = Objects.requireNonNull(modelTransform, "model transform");
        if (serverTick < 0L || playbackRevision < 1L) {
            throw new IllegalArgumentException("tick must be non-negative and revision positive");
        }
    }
}
