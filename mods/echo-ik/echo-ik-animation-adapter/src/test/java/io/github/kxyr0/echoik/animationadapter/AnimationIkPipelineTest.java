package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Bone;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import io.github.kxyr0.echoik.core.model.Transform;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import io.github.kxyr0.echoik.core.solver.TwoBoneSolver;
import io.github.kxyr0.echoik.runtime.InteractionTarget;
import io.github.kxyr0.echoik.runtime.InteractionTargetProvider;
import io.github.kxyr0.echoik.runtime.PoseConsumer;
import io.github.kxyr0.echoik.runtime.PoseProvider;
import io.github.kxyr0.echoik.runtime.TargetResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationIkPipelineTest {
    @Test
    void pullsPoseResolvesTargetAndPushesSolvedPose() {
        AtomicReference<Pose> accepted = new AtomicReference<>();
        PoseProvider poseProvider = subject -> Optional.of(pose());
        PoseConsumer poseConsumer = (subject, nextPose) -> accepted.set(nextPose);
        InteractionTargetProvider targetProvider = (subject, interaction) ->
                Optional.of(new InteractionTarget.WorldTarget("reach", new Vec3(1.0D, 1.0D, 0.0D),
                        Optional.empty(), 1.0D));
        TargetResolver resolver = (target, skeleton, currentPose) -> {
            var world = (InteractionTarget.WorldTarget) target;
            return Optional.of(new IkTarget("upper_arm", "forearm", "hand", world.position(),
                    world.rotation(), Optional.empty(), world.weight(), 1.0D, 1.0D));
        };
        var pipeline = new AnimationIkPipeline(poseProvider, poseConsumer, targetProvider, resolver,
                new TwoBoneSolver());

        AnimationIkReport report = pipeline.apply(new AnimationIkRequest("actor-a", "reach", skeleton()));

        Assertions.assertTrue(report.applied(), report.diagnostics().toString());
        Assertions.assertNotNull(accepted.get());
        Assertions.assertEquals(0.0D, accepted.get().requireTransform("hand")
                .translation().distanceTo(new Vec3(1.0D, 1.0D, 0.0D)), 0.0001D);
    }

    private SkeletonDefinition skeleton() {
        return new SkeletonDefinition(List.of(
                new Bone("upper_arm", Optional.empty(), Transform.identity(), 1.0D),
                new Bone("forearm", Optional.of("upper_arm"), Transform.identity(), 1.0D),
                new Bone("hand", Optional.of("forearm"), Transform.identity(), 0.0D)));
    }

    private Pose pose() {
        return new Pose(Map.of(
                "upper_arm", Transform.translated(new Vec3(0.0D, 0.0D, 0.0D)),
                "forearm", Transform.translated(new Vec3(1.0D, 0.0D, 0.0D)),
                "hand", Transform.translated(new Vec3(2.0D, 0.0D, 0.0D))));
    }
}
