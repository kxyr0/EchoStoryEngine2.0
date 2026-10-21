package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import io.github.kxyr0.echoik.core.solver.SolverContext;
import io.github.kxyr0.echoik.core.solver.TwoBoneSolver;
import io.github.kxyr0.echoik.runtime.InteractionTarget;
import io.github.kxyr0.echoik.runtime.InteractionTargetProvider;
import io.github.kxyr0.echoik.runtime.PoseConsumer;
import io.github.kxyr0.echoik.runtime.PoseProvider;
import io.github.kxyr0.echoik.runtime.TargetResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-injected bridge from a host animation pose to IK and back.
 */
public final class AnimationIkPipeline {
    private final PoseProvider poseProvider;
    private final PoseConsumer poseConsumer;
    private final InteractionTargetProvider targetProvider;
    private final TargetResolver targetResolver;
    private final TwoBoneSolver solver;

    public AnimationIkPipeline(
            PoseProvider poseProvider,
            PoseConsumer poseConsumer,
            InteractionTargetProvider targetProvider,
            TargetResolver targetResolver,
            TwoBoneSolver solver
    ) {
        this.poseProvider = Objects.requireNonNull(poseProvider, "poseProvider");
        this.poseConsumer = Objects.requireNonNull(poseConsumer, "poseConsumer");
        this.targetProvider = Objects.requireNonNull(targetProvider, "targetProvider");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.solver = Objects.requireNonNull(solver, "solver");
    }

    public AnimationIkReport apply(AnimationIkRequest request) {
        Optional<Pose> pose = poseProvider.currentPose(request.subjectId());
        if (pose.isEmpty()) {
            return rejected("Current pose is not available for subject: " + request.subjectId());
        }
        Optional<InteractionTarget> interaction = targetProvider.target(request.subjectId(), request.interactionId());
        if (interaction.isEmpty()) {
            return rejected("Interaction target is not available: " + request.interactionId());
        }
        Optional<IkTarget> target = targetResolver.resolve(interaction.orElseThrow(), request.skeleton(),
                pose.orElseThrow());
        if (target.isEmpty()) {
            return rejected("Interaction target could not be resolved to an IK target.");
        }
        var result = solver.solve(new SolverContext(request.skeleton(), pose.orElseThrow(), target.orElseThrow()));
        if (result.solved()) {
            poseConsumer.acceptPose(request.subjectId(), result.pose());
        }
        return new AnimationIkReport(result.solved(), Optional.of(result), result.diagnostics());
    }

    private AnimationIkReport rejected(String message) {
        return new AnimationIkReport(false, Optional.empty(), List.of(message));
    }
}
