package io.github.kxyr0.echoanimation.forge.client.skinning.pose;

import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedBone;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;

/** Calculates global pose and global-pose times inverse-bind in parent-first multiplication order. */
public final class SkinningPalette {
    public Matrix4f[] calculate(SkinnedModel model, AnimationPose pose, boolean freezeBindPose) {
        Map<String, Matrix4Transform> animated = animated(pose, freezeBindPose);
        Matrix4f[] globals = new Matrix4f[model.bones().size()];
        Matrix4f[] palette = new Matrix4f[model.bones().size()];
        for (int index = 0; index < palette.length; index++) {
            Matrix4f global = resolve(model, animated, globals, index);
            palette[index] = new Matrix4f(global).mul(model.bones().get(index).inverseBind());
        }
        return palette;
    }

    public Matrix4f global(SkinnedModel model, AnimationPose pose, String boneName, boolean freezeBindPose) {
        Matrix4f[] globals = new Matrix4f[model.bones().size()];
        Map<String, Matrix4Transform> animated = animated(pose, freezeBindPose);
        for (int index = 0; index < model.bones().size(); index++) {
            if (model.bones().get(index).name().equals(boneName)) {
                return new Matrix4f(resolve(model, animated, globals, index));
            }
        }
        throw new IllegalArgumentException("Skinned model " + model.source() + " has no bone " + boneName);
    }

    /**
     * Returns a model-space attachment socket frame. When cancelBindBasis is enabled,
     * the socket keeps the animated delta but removes the authored rest rotation/scale.
     */
    public Matrix4f socketGlobal(
            SkinnedModel model,
            AnimationPose pose,
            String boneName,
            boolean freezeBindPose,
            boolean cancelBindBasis) {

        Matrix4f posed =
                global(
                        model,
                        pose,
                        boneName,
                        freezeBindPose);

        if (!cancelBindBasis) {
            return posed;
        }

        Matrix4f bindBasis =
                global(
                        model,
                        pose,
                        boneName,
                        true);

        bindBasis
                .m30(0.0F)
                .m31(0.0F)
                .m32(0.0F);

        return posed.mul(
                bindBasis.invert());
    }

    public float[] uniformData(Matrix4f[] palette, int capacity) {
        float[] values = new float[capacity * 16];
        float[] matrix = new float[16];
        Matrix4f identity = new Matrix4f();
        for (int index = 0; index < capacity; index++) {
            (index < palette.length ? palette[index] : identity).get(matrix);
            System.arraycopy(matrix, 0, values, index * 16, matrix.length);
        }
        return values;
    }

    private Matrix4f resolve(
            SkinnedModel model,
            Map<String, Matrix4Transform> animated,
            Matrix4f[] globals,
            int index
    ) {
        if (globals[index] == null) {
            SkinnedBone bone = model.bones().get(index);
            Matrix4f local = animated.containsKey(bone.name())
                    ? matrix(animated.get(bone.name())) : bone.bindLocal();
            globals[index] = bone.parentIndex() < 0 ? local
                    : new Matrix4f(resolve(model, animated, globals, bone.parentIndex())).mul(local);
        }
        return globals[index];
    }

    private Map<String, Matrix4Transform> animated(AnimationPose pose, boolean freezeBindPose) {
        Map<String, Matrix4Transform> result = new HashMap<>();
        if (!freezeBindPose) {
            pose.transforms().forEach((bone, transform) ->
                    result.put(bone.value(), Matrix4Transform.fromEuler(transform)));
            pose.matrixTransforms().forEach((bone, transform) -> result.put(bone.value(), transform));
        }
        return result;
    }

    private Matrix4f matrix(Matrix4Transform transform) {
        return new Matrix4f().set(transform.columnMajorFloats());
    }
}
