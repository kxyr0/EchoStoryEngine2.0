package io.github.kxyr0.echoanimation.forge.playback.result;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import java.util.Optional;
public final class AnimationPlayFailures {
    private AnimationPlayFailures() { }
    public static AnimationPlayResult postureLocked( AnimationId animationId, AnimationSubjectId subjectId) {
        return new AnimationPlayResult( AnimationPlayResult.Status.CHANNEL_CONFLICT, animationId, subjectId,
                Optional.empty(), Optional.empty(), "EANIM-BODY-POSTURE-LOCK", "BODY playback is locked by posture.");
    }

    public static AnimationPlayResult unavailable(AnimationId animationId, AnimationSubjectId subjectId) {
        return new AnimationPlayResult( AnimationPlayResult.Status.DEFINITION_UNAVAILABLE, animationId, subjectId,
                Optional.empty(), Optional.empty(), "EANIM-A3-UNAVAILABLE", "Echo Animation service is unavailable.");
    }

}
