package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import java.util.Objects;

/**
 * Request to apply IK over the current pose owned by a host animation system.
 */
public record AnimationIkRequest(String subjectId, String interactionId, SkeletonDefinition skeleton) {
    public AnimationIkRequest {
        subjectId = required(subjectId, "subjectId");
        interactionId = required(interactionId, "interactionId");
        skeleton = Objects.requireNonNull(skeleton, "skeleton");
    }

    private static String required(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
