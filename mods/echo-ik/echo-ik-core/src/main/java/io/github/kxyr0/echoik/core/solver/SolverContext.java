package io.github.kxyr0.echoik.core.solver;

import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import java.util.Objects;

/**
 * Input bundle for one solver invocation.
 */
public record SolverContext(SkeletonDefinition skeleton, Pose currentPose, IkTarget target) {
    public SolverContext {
        skeleton = Objects.requireNonNull(skeleton, "skeleton");
        currentPose = Objects.requireNonNull(currentPose, "currentPose");
        target = Objects.requireNonNull(target, "target");
    }
}
