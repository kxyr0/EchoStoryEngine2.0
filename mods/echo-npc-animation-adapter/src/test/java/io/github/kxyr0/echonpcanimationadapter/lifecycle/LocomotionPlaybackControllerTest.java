package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCatalogState;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class LocomotionPlaybackControllerTest {
    @Test
    void adoptsPersistedWalkPlaybackWithoutRestarting() {
        FakeAnimationService animation = new FakeAnimationService(true);
        LocomotionPlaybackController controller = new LocomotionPlaybackController(
                lifecycle(), new AnimationSubjectMapping());

        controller.startOrUpdate(null, animation, "jdh:father", "jdh:move", 1L,
                NavigationGait.WALK, 1.2D);

        Assertions.assertEquals(0, animation.playCalls);
        Assertions.assertEquals(1, animation.speedCalls);
    }

    @Test
    void stopsPersistedWalkPlaybackWithoutLocalPlayingFlag() {
        FakeAnimationService animation = new FakeAnimationService(true);
        LocomotionPlaybackController controller = new LocomotionPlaybackController(
                lifecycle(), new AnimationSubjectMapping());

        controller.stop(animation, "jdh:father", "orphan/reconcile");

        Assertions.assertEquals(2, animation.stopCalls);
    }

    private NpcAnimationLifecycleAdapter lifecycle() {
        return new NpcAnimationLifecycleAdapter(new NpcAnimationBindingCatalogState());
    }

    private static final class FakeAnimationService implements AnimationService {
        private final boolean active;
        private int playCalls;
        private int stopCalls;
        private int speedCalls;

        private FakeAnimationService(boolean active) {
            this.active = active;
        }

        @Override
        public java.util.List<io.github.kxyr0.echoanimation.api.id.FaceRigId> faceRigs() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<io.github.kxyr0.echoanimation.api.id.FacePoseId> facePoses() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<io.github.kxyr0.echoanimation.api.id.FacialAnimationId> facialAnimations() {
            return java.util.List.of();
        }

        @Override
        public AnimationPlayResult playAnimation(AnimationId animationId, AnimationSubjectId subjectId,
                AnimationCommandId commandId, double playbackSpeed) {
            playCalls++;
            return new AnimationPlayResult(AnimationPlayResult.Status.STARTED, animationId, subjectId,
                    Optional.empty(), Optional.empty(), "test", "started");
        }

        @Override
        public AnimationStopResult stopAnimation(AnimationSubjectId subjectId, Optional<AnimationId> animationId,
                AnimationCommandId commandId) {
            stopCalls++;
            return new AnimationStopResult(AnimationStopResult.Status.STOP_REQUESTED, subjectId,
                    Optional.empty(), "test", "stopped");
        }

        @Override
        public boolean updateAnimationPlaybackSpeed(AnimationSubjectId subjectId, AnimationId animationId,
                AnimationCommandId commandId, double playbackSpeed) {
            speedCalls++;
            return true;
        }

        @Override
        public AnimationPlaybackQueryResult queryAnimation(AnimationSubjectId subjectId, AnimationId animationId) {
            return new AnimationPlaybackQueryResult(active ? AnimationPlaybackQueryResult.Status.ACTIVE
                    : AnimationPlaybackQueryResult.Status.PLAYBACK_NOT_FOUND, animationId, subjectId,
                    Optional.empty(), Optional.empty(), Optional.empty(), 1L, 1L, "test", "query");
        }
    }
}
