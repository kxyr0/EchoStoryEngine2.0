package io.github.kxyr0.echoik.runtime;

import io.github.kxyr0.echoik.core.model.Pose;
import java.util.Optional;

/**
 * Provides the current animation-space pose for a subject.
 */
public interface PoseProvider {
    Optional<Pose> currentPose(String subjectId);
}
