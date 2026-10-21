package io.github.kxyr0.echoanimation.api.provisioning;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Public owner namespace for automatically provisioned subjects.
 */
public record AnimationSubjectLifecycleOwnerId(String value)
        implements Comparable<AnimationSubjectLifecycleOwnerId> {
    public AnimationSubjectLifecycleOwnerId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationSubjectLifecycleOwnerId");
    }

    public static AnimationSubjectLifecycleOwnerId parse(String value) {
        return new AnimationSubjectLifecycleOwnerId(value);
    }

    @Override
    public int compareTo(AnimationSubjectLifecycleOwnerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
