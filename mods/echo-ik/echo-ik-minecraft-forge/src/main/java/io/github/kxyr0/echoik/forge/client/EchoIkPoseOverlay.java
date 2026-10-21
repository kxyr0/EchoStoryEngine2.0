package io.github.kxyr0.echoik.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.forge.api.client.render.pose.AnimationHumanoidPoseOverlayEvent;
import io.github.kxyr0.echoik.animationadapter.EchoIkChain;
import io.github.kxyr0.echoik.animationadapter.EchoPoseReader;
import io.github.kxyr0.echoik.animationadapter.EchoPoseWriter;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import io.github.kxyr0.echoik.core.solver.SolverContext;
import io.github.kxyr0.echoik.core.solver.TwoBoneSolver;
import io.github.kxyr0.echoik.forge.runtime.IkConstraint;
import io.github.kxyr0.echoik.forge.runtime.IkRuntimeState;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Applies right-arm IK after Echo Animation sampling and before humanoid rendering.
 */
public final class EchoIkPoseOverlay {
    private final IkRuntimeState state;
    private final EchoPoseReader reader;
    private final EchoPoseWriter writer;
    private final TwoBoneSolver solver;
    private final EchoIkDebugRenderer debugRenderer;
    private final EchoIkChain rightArm;
    private final Logger logger = LogUtils.getLogger();

    public EchoIkPoseOverlay(IkRuntimeState state) {
        this(state, new EchoPoseReader(), new EchoPoseWriter(), new TwoBoneSolver(),
                new EchoIkDebugRenderer(), new EchoIkChain("right_upper_arm", "right_forearm", "main_hand"));
    }

    EchoIkPoseOverlay(
            IkRuntimeState state,
            EchoPoseReader reader,
            EchoPoseWriter writer,
            TwoBoneSolver solver,
            EchoIkDebugRenderer debugRenderer,
            EchoIkChain rightArm
    ) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.reader = java.util.Objects.requireNonNull(reader, "reader");
        this.writer = java.util.Objects.requireNonNull(writer, "writer");
        this.solver = java.util.Objects.requireNonNull(solver, "solver");
        this.debugRenderer = java.util.Objects.requireNonNull(debugRenderer, "debugRenderer");
        this.rightArm = java.util.Objects.requireNonNull(rightArm, "rightArm");
    }

    public void onPoseOverlay(AnimationHumanoidPoseOverlayEvent event) {
        Optional<IkConstraint> constraint = state.constraint(event.subjectId().value());
        if (constraint.isEmpty() || constraint.orElseThrow().currentWeight() <= 0.0D) {
            return;
        }
        try {
            apply(event, constraint.orElseThrow());
        } catch (RuntimeException exception) {
            logger.warn("Echo IK overlay пропущен для {}: {}", event.subjectId(), exception.getMessage());
        }
    }

    private void apply(AnimationHumanoidPoseOverlayEvent event, IkConstraint constraint) {
        Pose currentPose = reader.read(event.rig(), event.localTransforms());
        IkTarget target = new IkTarget(rightArm.rootBoneId(), rightArm.midBoneId(), rightArm.endEffectorId(),
                constraint.targetPosition(), Optional.empty(), constraint.poleTarget(),
                constraint.currentWeight(), 0.0D, 1.0D);
        var result = solver.solve(new SolverContext(reader.skeleton(event.rig()), currentPose, target));
        if (result.solved()) {
            writer.write(event.rig(), event.localTransforms(), result.pose(), rightArm)
                    .ifPresent(event::applyOverlay);
        }
        if (state.debugEnabled(event.subjectId().value())) {
            debugRenderer.render(event, result.pose(), target);
        }
    }
}
