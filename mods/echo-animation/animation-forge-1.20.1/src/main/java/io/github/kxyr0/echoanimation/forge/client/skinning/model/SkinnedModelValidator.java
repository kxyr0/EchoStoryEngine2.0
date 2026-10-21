package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import java.util.HashSet;
import java.util.Set;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/** Structural and bind-pose validation performed before any GPU upload. */
public final class SkinnedModelValidator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float EPSILON = 0.001F;

    public void validate(SkinnedModel model, int boneLimit) {
        if (model.formatVersion() < 1 || model.formatVersion() > 2) {
            throw error(model, "unsupported formatVersion " + model.formatVersion());
        }
        if (model.bones().isEmpty() || model.bones().size() > boneLimit) {
            throw error(model, "bone count must be between 1 and " + boneLimit);
        }
        validateBones(model);
        validateAttachmentBindings(model);
        validateMeshes(model);
        validateBindPose(model);
    }

    private void validateBones(SkinnedModel model) {
        Set<String> names = new HashSet<>();
        int roots = 0;
        for (int index = 0; index < model.bones().size(); index++) {
            SkinnedBone bone = model.bones().get(index);
            if (!names.add(bone.name())) {
                throw error(model, "duplicate bone " + bone.name());
            }
            if (bone.parentIndex() == -1) {
                roots++;
            } else if (bone.parentIndex() < 0 || bone.parentIndex() >= model.bones().size()) {
                throw error(model, "invalid parent for bone " + bone.name());
            }
            visit(model, index, new HashSet<>());
        }
        if (roots < 1) {
            throw error(model, "skeleton must contain at least one root");
        }
    }

    private void visit(SkinnedModel model, int index, Set<Integer> path) {
        if (!path.add(index)) {
            throw error(model, "cyclic skeleton at bone " + model.bones().get(index).name());
        }
        int parent = model.bones().get(index).parentIndex();
        if (parent >= 0) {
            visit(model, parent, path);
        }
        path.remove(index);
    }

    private void validateAttachmentBindings(SkinnedModel model) {
        SkinnedAttachmentBindings bindings = model.attachmentBindings();
        if (!bindings.explicit()) {
            return;
        }
        if (bindings.cancelBindBasis()
                && bindings.space()
                != AnimationAttachmentTransformSpace.MODEL_ABSOLUTE) {
            throw error(
                    model,
                    "cancelBindBasis requires MODEL_ABSOLUTE attachment space");
        }
        Set<String> names = new HashSet<>();
        model.bones().forEach(bone -> names.add(bone.name()));
        for (String bone : java.util.List.of(
                bindings.torsoBackBone(),
                bindings.leftHandBone(),
                bindings.rightHandBone())) {
            if (!names.contains(bone)) {
                throw error(model, "attachment binding references missing bone " + bone);
            }
        }
    }

    private void validateMeshes(SkinnedModel model) {
        for (SkinnedMesh mesh : model.meshes()) {
            if (mesh.triangles().length % 3 != 0) {
                throw error(model, "mesh " + mesh.name() + " index count is not divisible by three");
            }
            for (int triangleIndex : mesh.triangles()) {
                if (triangleIndex < 0 || triangleIndex >= mesh.vertices().size()) {
                    throw error(model, "mesh " + mesh.name() + " has invalid vertex index " + triangleIndex);
                }
            }
            for (int vertexIndex = 0; vertexIndex < mesh.vertices().size(); vertexIndex++) {
                validateSkin(model, mesh, vertexIndex);
            }
        }
    }

    private void validateSkin(SkinnedModel model, SkinnedMesh mesh, int vertexIndex) {
        SkinWeights skin = mesh.vertices().get(vertexIndex).skin();
        float sum = 0.0F;
        for (int slot = 0; slot < SkinWeights.LIMIT; slot++) {
            if (skin.index(slot) < 0 || skin.index(slot) >= model.bones().size()) {
                throw error(model, "mesh " + mesh.name() + " vertex " + vertexIndex + " has invalid bone");
            }
            sum += skin.weight(slot);
        }
        if (Math.abs(sum - 1.0F) > EPSILON) {
            throw error(model, "mesh " + mesh.name() + " vertex " + vertexIndex + " weights sum to " + sum);
        }
    }

    private void validateBindPose(SkinnedModel model) {
        Matrix4f[] globals = new Matrix4f[model.bones().size()];
        float worstError = 0.0F;
        String worstBone = model.bones().get(0).name();
        for (int index = 0; index < globals.length; index++) {
            bindGlobal(model, globals, index);
            Matrix4f restored = new Matrix4f(globals[index]).mul(model.bones().get(index).inverseBind());
            float maxError = identityError(restored);
            String bone = model.bones().get(index).name();
            LOGGER.info("Bind-pose check model={} bone={} maxError={}", model.source(), bone, maxError);
            if (maxError > worstError) {
                worstError = maxError;
                worstBone = bone;
            }
            if (!Float.isFinite(maxError) || maxError > EPSILON) {
                throw error(model, "inverse bind mismatch for bone " + bone + ", maxError=" + maxError);
            }
        }
        LOGGER.info("Bind-pose check passed model={} bones={} worstBone={} maxError={}", model.source(),
                globals.length, worstBone, worstError);
    }

    private Matrix4f bindGlobal(SkinnedModel model, Matrix4f[] globals, int index) {
        if (globals[index] == null) {
            SkinnedBone bone = model.bones().get(index);
            Matrix4f local = bone.bindLocal();
            globals[index] = bone.parentIndex() < 0 ? local
                    : new Matrix4f(bindGlobal(model, globals, bone.parentIndex())).mul(local);
        }
        return globals[index];
    }

    private float identityError(Matrix4f matrix) {
        float[] values = matrix.get(new float[16]);
        float maxError = 0.0F;
        for (int index = 0; index < values.length; index++) {
            float expected = index % 5 == 0 ? 1.0F : 0.0F;
            if (!Float.isFinite(values[index])) {
                return Float.NaN;
            }
            maxError = Math.max(maxError, Math.abs(values[index] - expected));
        }
        return maxError;
    }

    private IllegalArgumentException error(SkinnedModel model, String message) {
        return new IllegalArgumentException("Skinned model " + model.source() + ": " + message);
    }
}
