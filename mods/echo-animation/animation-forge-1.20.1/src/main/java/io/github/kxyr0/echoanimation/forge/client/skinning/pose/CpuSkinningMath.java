package io.github.kxyr0.echoanimation.forge.client.skinning.pose;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Reference LBS implementation used by tests and the explicit CPU fallback. */
public final class CpuSkinningMath {
    public Vector3f position(SkinnedVertex vertex, Matrix4f[] palette) {
        Vector3f result = new Vector3f();
        SkinWeights skin = vertex.skin();
        for (int slot = 0; slot < SkinWeights.LIMIT; slot++) {
            float weight = skin.weight(slot);
            if (weight > 0.0F) {
                Vector3f point = new Vector3f(vertex.x(), vertex.y(), vertex.z());
                palette[skin.index(slot)].transformPosition(point);
                result.fma(weight, point);
            }
        }
        return result;
    }

    public Vector3f normal(SkinnedVertex vertex, Matrix4f[] palette) {
        Vector3f result = new Vector3f();
        SkinWeights skin = vertex.skin();
        for (int slot = 0; slot < SkinWeights.LIMIT; slot++) {
            float weight = skin.weight(slot);
            if (weight > 0.0F) {
                Vector3f normal = new Vector3f(vertex.normalX(), vertex.normalY(), vertex.normalZ());
                palette[skin.index(slot)].transformDirection(normal);
                result.fma(weight, normal);
            }
        }
        return result.normalize();
    }
}
