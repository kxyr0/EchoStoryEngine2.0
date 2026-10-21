package io.github.kxyr0.echoanimation.api.clip.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.Objects;

/** Declares how the authored root track is consumed by runtime integration. */
public record AnimationRootMotion(Mode mode, BoneId boneId) {
    public enum Mode {
        DISABLED,
        EXTRACT,
        PRESERVE
    }

    public AnimationRootMotion {
        mode = Objects.requireNonNull(mode, "mode");
        boneId = Objects.requireNonNull(boneId, "boneId");
    }

    public static AnimationRootMotion disabled() {
        return new AnimationRootMotion(Mode.DISABLED, BoneId.parse("root"));
    }
}
