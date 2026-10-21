package io.github.kxyr0.echoanimation.api.rig;

import io.github.kxyr0.echoanimation.api.id.BoneId;

/**
 * Local attachment socket identity within one rig.
 */
public record AnimationAttachmentSocketId(String value) implements Comparable<AnimationAttachmentSocketId> {
    public AnimationAttachmentSocketId {
        new BoneId(value);
    }

    public static AnimationAttachmentSocketId parse(String value) {
        return new AnimationAttachmentSocketId(value);
    }

    @Override
    public int compareTo(AnimationAttachmentSocketId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
