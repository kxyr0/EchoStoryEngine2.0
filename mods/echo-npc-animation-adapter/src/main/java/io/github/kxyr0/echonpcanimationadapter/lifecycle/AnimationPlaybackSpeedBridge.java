package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class AnimationPlaybackSpeedBridge {
    private AnimationPlaybackSpeedBridge() {
    }

    static AnimationPlayResult play(
            AnimationService animation,
            AnimationId animationId,
            AnimationSubjectId subjectId,
            AnimationCommandId commandId,
            double playbackSpeed
    ) {
        try {
            Method method = AnimationService.class.getMethod("playAnimation", AnimationId.class,
                    AnimationSubjectId.class, AnimationCommandId.class, double.class);
            return (AnimationPlayResult) method.invoke(animation, animationId, subjectId, commandId, playbackSpeed);
        } catch (NoSuchMethodException exception) {
            return animation.playAnimation(animationId, subjectId, commandId);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return animation.playAnimation(animationId, subjectId, commandId);
        }
    }

    static boolean update(
            AnimationService animation,
            AnimationSubjectId subjectId,
            AnimationId animationId,
            AnimationCommandId commandId,
            double playbackSpeed
    ) {
        try {
            Method method = AnimationService.class.getMethod("updateAnimationPlaybackSpeed",
                    AnimationSubjectId.class, AnimationId.class, AnimationCommandId.class, double.class);
            return (boolean) method.invoke(animation, subjectId, animationId, commandId, playbackSpeed);
        } catch (NoSuchMethodException exception) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }
}
