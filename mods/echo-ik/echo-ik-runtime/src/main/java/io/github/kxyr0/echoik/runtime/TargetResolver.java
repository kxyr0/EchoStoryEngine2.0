package io.github.kxyr0.echoik.runtime;

import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import java.util.Optional;

/**
 * Converts interaction targets into concrete IK chain targets.
 */
public interface TargetResolver {
    Optional<IkTarget> resolve(InteractionTarget target, SkeletonDefinition skeleton, Pose currentPose);
}
