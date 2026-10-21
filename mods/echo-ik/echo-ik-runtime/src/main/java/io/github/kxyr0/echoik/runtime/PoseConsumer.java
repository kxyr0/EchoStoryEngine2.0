package io.github.kxyr0.echoik.runtime;

import io.github.kxyr0.echoik.core.model.Pose;

/**
 * Receives the final pose after IK.
 */
public interface PoseConsumer {
    void acceptPose(String subjectId, Pose pose);
}
