package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import java.util.Objects;
import org.joml.Matrix4f;

/** One model-space skeleton entry with local bind and inverse global bind matrices. */
public record SkinnedBone(String name, int parentIndex, Matrix4f bindLocal, Matrix4f inverseBind) {
    public SkinnedBone {
        name = Objects.requireNonNull(name, "name");
        bindLocal = new Matrix4f(Objects.requireNonNull(bindLocal, "bindLocal"));
        inverseBind = new Matrix4f(Objects.requireNonNull(inverseBind, "inverseBind"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("Bone name must not be blank.");
        }
    }

    @Override
    public Matrix4f bindLocal() {
        return new Matrix4f(bindLocal);
    }

    @Override
    public Matrix4f inverseBind() {
        return new Matrix4f(inverseBind);
    }
}
